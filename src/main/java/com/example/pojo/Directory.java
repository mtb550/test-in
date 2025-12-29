package com.example.pojo;

import java.io.File;

public interface Directory {
    File getFile();

    void setFile(File file);

    String getName();

    void setName(String name);
}
