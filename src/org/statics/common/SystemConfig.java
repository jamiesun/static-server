package org.statics.common;

public interface SystemConfig
{

    public static final int NODE_PORT_DISPLACEMENT = 100;

    public static final String DEFAULT_ENCODE = "utf-8";

    public static final char QUALITY_ORIGINAL = 'O';
    public static final char QUALITY_SMALL = 'S';
    public static final char QUALITY_MEDIUM = 'M';
    public static final char QUALITY_HIGH = 'H';

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int NOT_MASTER = 2;

    public static final int IDENTIFIER_ADD = 100;
    public static final int IDENTIFIER_ADD_ACK = 101;

    public static final int IDENTIFIER_DEL = 102;
    public static final int IDENTIFIER_DEL_ACK = 103;

    public static final int IDENTIFIER_GET = 104;
    public static final int IDENTIFIER_GET_ACK = 105;

    public static final int IDENTIFIER_PING = 106;
    public static final int IDENTIFIER_PING_ACK = 107;
}
