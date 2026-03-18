package testGit.pojo.tree.dirs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestSetPackageDirectory extends Directory {
    @Override
    public TestSetPackageDirectory setPath(Path path) {
        super.setPath(path);
        return this;
    }

    @Override
    public TestSetPackageDirectory setName(String name) {
        super.setName(name);
        return this;
    }

}
