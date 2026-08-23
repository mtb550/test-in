package org.testin.sftp;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.testin.git.TestCaseMerge;

import java.util.List;

/**
 * One test case a sync could not finish on its own (#94).
 * <p>
 * Everything the merge could settle is settled by the time this exists - the
 * fields only one side touched, the audit stamps, the order pointers. What is
 * left is a field both testers rewrote, which is a question rather than a
 * calculation, and a question has to reach a person.
 *
 * @param path      where the case lives, relative to the project
 * @param name      what the case is called, for the dialog title. A tester
 *                  answering three of these in a row needs to know which one
 *                  they are looking at
 * @param merged    everything settled so far, waiting for the answers
 * @param questions the fields both sides rewrote
 * @param theirs    the server's whole version, because an answer of "theirs"
 *                  is read back out of it field by field
 */
public record Unsettled(@NotNull String path, @NotNull String name, @NotNull ObjectNode merged,
                        @NotNull List<TestCaseMerge.Question> questions, @NotNull String theirs) {
}
