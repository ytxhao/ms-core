package media.ushow.zorro;

public class Constants {
  public static final int CHANNEL_PROFILE_VOICE_CHAT_ROOM = 0; // 语音聊天室场景
  public static final int CHANNEL_PROFILE_LIVE_CALL = 1;       // 直播场景：主播直播/连麦/PK，观众连麦

  public static final int CLIENT_ROLE_AUDIENCE = 0; // 语音聊天室场景默认角色
  public static final int CLIENT_ROLE_BROADCASTER = 1; // 直播场景直播场景默认角色

  public static final int CONNECTION_STATE_DISCONNECTED = 0;
  public static final int CONNECTION_STATE_CONNECTING = 1;
  public static final int CONNECTION_STATE_CONNECTED = 2;
  public static final int CONNECTION_STATE_RECONNECTING = 3;
  public static final int CONNECTION_STATE_FAILED = 4;

  public static final int CONNECTION_CHANGED_CONNECTING = 0;
  public static final int CONNECTION_CHANGED_JOIN_SUCCESS = 1;
  public static final int CONNECTION_CHANGED_INTERRUPTED = 2;
  public static final int CONNECTION_CHANGED_BANNED_BY_SERVER = 3;
  public static final int CONNECTION_CHANGED_JOIN_FAILED = 4;
  public static final int CONNECTION_CHANGED_LEAVE_CHANNEL = 5;
  public static final int CONNECTION_CHANGED_INVALID_APP_ID = 6;
  public static final int CONNECTION_CHANGED_INVALID_CHANNEL_NAME = 7;
  public static final int CONNECTION_CHANGED_INVALID_TOKEN = 8;
  public static final int CONNECTION_CHANGED_TOKEN_EXPIRED = 9;
  public static final int CONNECTION_CHANGED_REJECTED_BY_SERVER = 10;
  public static final int CONNECTION_CHANGED_SETTING_PROXY_SERVER = 11;
  public static final int CONNECTION_CHANGED_RENEW_TOKEN = 12;
  public static final int CONNECTION_CHANGED_CLIENT_IP_ADDRESS_CHANGED = 13;
  public static final int CONNECTION_CHANGED_KEEP_ALIVE_TIMEOUT = 14;

  public static final int ERR_OK = 0;
  public static final int ERR_FAILED = 1;
  public static final int ERR_INVALID_ARGUMENT = 2;
  public static final int ERR_NOT_READY = 3;
  public static final int ERR_NOT_SUPPORTED = 4;
  public static final int ERR_BUFFER_TOO_SMALL = 5;
  public static final int ERR_NOT_INITIALIZED = 6;
  public static final int ERR_NO_PERMISSION = 7;
  public static final int ERR_TIMEDOUT = 8;
  public static final int ERR_CANCELED = 9;
  public static final int ERR_JOIN_CHANNEL_REJECTED = 10;
  public static final int ERR_LEAVE_CHANNEL_REJECTED = 11;
  public static final int ERR_KICK_BY_REMOTE = 12;
  public static final int ERR_INVALID_APP_ID = 13;
  public static final int ERR_START_LIVE_PK_FAILED = 14;
  public static final int ERR_LIVE_PK_ERROR = 15;
}

