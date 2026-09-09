package org.testin.notifications;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * What Testin says when it will not do what was asked.
 * <p>
 * The other half of {@link Done}, and here for the same reason: a sentence a
 * tester reads is vocabulary, and vocabulary needs an owner. These five lived on
 * {@link Notifier} as a method each, which made the class that delivers messages
 * also the class that writes them - so the one place to look for how Testin
 * refuses was a list of balloon plumbing with sentences scattered through it.
 * <p>
 * Together on purpose. Three of these answer nearly the same gesture, and the
 * only way to tell whether a sixth is needed is to read them beside each other:
 * a test run with nothing left in it, a screen a filter has emptied, and a run
 * already going are three different things to say, and saying the wrong one
 * sends the tester looking for test cases that are still there (#215).
 * <p>
 * Every one of them fades. A refusal is feedback on the gesture just made, not a
 * failure worth keeping beside real ones (#62).
 */
@Getter
@AllArgsConstructor
public enum Refused {

    /**
     * Rule-TREE-PANEL-004.
     * <p>
     * The tester typed a name that is already taken, whichever action they
     * reached it through - creating a test project, creating any node in the
     * tree, or renaming one.
     */
    ALREADY_EXISTS("%s Already Exists"),

    /**
     * The tester acted on a test case that has no generated method, whichever
     * action they reached it through - running the case, or editing it and
     * expecting the code to follow. Both used to give up in silence, each in its
     * own way: the runner ran whatever else the class held, and an edit changed
     * the case and left the code alone (#34, #66 finding 19).
     * <p>
     * The remedy - generate the code - is a keystroke away, which is why this
     * fades rather than going in the log.
     */
    NO_GENERATED_CODE("%s has no generated code yet"),

    /**
     * UC-TREE-PANEL-023, Rule-TREE-PANEL-078.
     * <p>
     * The tester asked to run a node that has nothing left to run: a test set
     * holding no cases at all, or a run whose cases have all been judged. Both
     * are Testin saying there is nothing here to start.
     */
    NOTHING_TO_RUN("%s has no test cases to run"),

    /**
     * The tester pressed start on a walk with nowhere to land.
     * <p>
     * Its own sentence rather than {@link #NOTHING_TO_RUN}: that one is about
     * what the test run holds, this one about what is on screen. Naming the
     * filter is the whole difference - telling a tester their test run has no
     * test cases, when a filter is what emptied the screen, sends them looking
     * for cases that are still there (#215).
     */
    NOTHING_SHOWING("Nothing showing in %s is waiting for a verdict"),

    /**
     * The tester asked to run something that is already running.
     * <p>
     * Its own sentence rather than {@link #NOTHING_TO_RUN}: a run with cases
     * still going has plenty left to run, and telling them it has nothing would
     * send them looking for cases that are on screen in front of them.
     */
    ALREADY_RUNNING("%s is already running"),

    /**
     * UC-CODEGEN-019, Rule-CODEGEN-005.
     * <p>
     * The IDE is still building its index, so no class can be found by name -
     * {@code JavaPsiFacade.findClass} answers that with an exception, which
     * would reach the tester as an internal error during an action they did not
     * know touched the index (#126).
     * <p>
     * Refused rather than deferred, and the difference matters for one
     * operation in particular. Deferring would be right for a create and wrong
     * for a rename or a move: Rule-CODEGEN-004 has those run while the old name
     * still finds the code, so a rename that waits for the index runs after the
     * tree has changed and looks for a class that no longer answers to that
     * name.
     */
    WHILE_INDEXING("%s needs the IDE to finish indexing first");

    /**
     * The sentence, with one slot for whatever the tester acted on.
     */
    private final @NotNull String sentence;

    /**
     * The sentence about one thing - a node's name, a test case's description.
     */
    public @NotNull String about(final @NotNull String name) {
        return sentence.formatted(name);
    }
}
