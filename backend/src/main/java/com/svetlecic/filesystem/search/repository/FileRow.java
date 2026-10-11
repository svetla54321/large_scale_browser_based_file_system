package com.svetlecic.filesystem.search.repository;

public record FileRow(long id, String name, long parentId, String ancestorPath) {

}
