package com.svetlecic.filesystem;

import org.springframework.boot.SpringApplication;

public class TestFileSystemApplication {

    static void main(String[] args) {
        SpringApplication.from(FileSystemApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
