package testGit.ui;

import testGit.pojo.DirectoryType;
import testGit.pojo.tree.dirs.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DirectoryOptions {
    private final Class<?>[] availableItems = {
            TestSetPackageDirectory.class,
            TestRunPackageDirectory.class,
            TestSetDirectory.class,
            TestRunDirectory.class,
            TestProjectDirectory.class
    };

    private final Map<Class<?>, Boolean> activeStates = new HashMap<>();

    public DirectoryOptions() {
        for (Class<?> item : availableItems) {
            activeStates.put(item, true);
        }
    }

    public TypeConfigurator type(DirectoryType type) {
        return new TypeConfigurator(type.getClazz());
    }

    public Class<?>[] getItems() {
        return availableItems;
    }

    public Predicate<Class<?>> getDisabledPredicate() {
        return type -> !activeStates.getOrDefault(type, true);
    }

    public class TypeConfigurator {
        private final Class<?> currentType;

        public TypeConfigurator(Class<?> currentType) {
            this.currentType = currentType;
        }

        public DirectoryOptions setActive() {
            activeStates.put(currentType, true);
            return DirectoryOptions.this;
        }

        public DirectoryOptions setInactive() {
            activeStates.put(currentType, false);
            return DirectoryOptions.this;
        }

        public DirectoryOptions setStatus(boolean status) {
            activeStates.put(currentType, status);
            return DirectoryOptions.this;
        }
    }
}