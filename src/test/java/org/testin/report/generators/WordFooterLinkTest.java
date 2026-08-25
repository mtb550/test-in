package org.testin.report.generators;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * The plugin's name in the Word footer is a link, and survives being written to
 * a file and read back.
 * <p>
 * A hyperlink in Word is not a property of a run: it is a relationship stored in
 * the part that holds the paragraph, and a footer is its own part. So a link
 * that looks right in memory can still be written into a document that carries
 * no relationship for it - the text stays, the link quietly does not. Nothing
 * fails when that happens, which is why this goes through the round trip.
 */
public class WordFooterLinkTest {

    @Test
    public void theFooterCarriesAClickableLink() {
        try (ByteArrayOutputStream saved = new ByteArrayOutputStream()) {
            try (XWPFDocument written = new XWPFDocument()) {
                final @NotNull XWPFFooter footer =
                        written.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
                final @NotNull XWPFParagraph line = footer.createParagraph();

                line.createRun().setText("Generated automatically by ");
                final @NotNull XWPFHyperlinkRun link = line.createHyperlinkRun(ReportText.PLUGIN_URL);
                link.setText("Testin");
                line.createRun().setText(" IntelliJ plugin.");

                written.write(saved);
            }

            try (XWPFDocument read = new XWPFDocument(new ByteArrayInputStream(saved.toByteArray()))) {
                final @NotNull XWPFFooter reopened = read.getFooterList().getFirst();
                final @NotNull XWPFParagraph line = reopened.getParagraphs().getFirst();

                final @NotNull StringBuilder linked = new StringBuilder();
                for (final XWPFRun run : line.getRuns()) {
                    if (run instanceof XWPFHyperlinkRun hyperlink) {
                        linked.append(run.text());

                        // Asked of the footer's own part, not the document's. A
                        // footer is a part in its own right and its relationships
                        // live there, so getHyperlink(document) answers null for a
                        // link that is perfectly well stored - which is the trap
                        // this test exists to sit in.
                        final @NotNull String target = reopened.getPackagePart()
                                .getRelationship(hyperlink.getHyperlinkId()).getTargetURI().toString();

                        assertEquals(target, ReportText.PLUGIN_URL,
                                "the link points somewhere other than the plugin page");
                    }
                }

                assertEquals(linked.toString(), "Testin",
                        "the linked words should be the plugin's name and nothing else");
                assertTrue(line.getText().contains("IntelliJ plugin."),
                        "the rest of the sentence should still be there, unlinked");
            }

        } catch (final Exception cannotRun) {
            fail("Could not round-trip the footer: " + cannotRun.getMessage(), cannotRun);
        }
    }
}
