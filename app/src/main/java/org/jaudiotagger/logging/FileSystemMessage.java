package org.jaudiotagger.logging;

/*
 * For parsing the exact cause of a file exception, because variations not handled well by Java
 */
@SuppressWarnings("SameParameterValue")
public enum FileSystemMessage {
    ACCESS_IS_DENIED("Access is denied"),;
    final String msg;

    FileSystemMessage(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
