package com.svetlecic.filesystem;

import org.springframework.boot.SpringApplication;

public class TestFileSystemApplication {

    public static void main(String[] args) {
        SpringApplication.from(FileSystemApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
