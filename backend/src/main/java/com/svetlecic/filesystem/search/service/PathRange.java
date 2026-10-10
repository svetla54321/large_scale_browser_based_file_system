package com.svetlecic.filesystem.search.service;

import com.svetlecic.filesystem.node.domain.Node;

record PathRange(String from, String to) {

    static final PathRange EVERYWHERE = new PathRange("/", "0");

    static PathRange below(Node folder) {
        var prefix = folder.getAncestorPath() + folder.getId() + "/";
        return new PathRange(prefix, prefix.substring(0, prefix.length() - 1) + "0");
    }
}
