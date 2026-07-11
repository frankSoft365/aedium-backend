package com.microsoft.aediumbackend.constant;

public class ErrorDescriptionConstant {
    public static final String SYSTEM_INTERNAL_ERROR = "系统内部错误";
    public static final String INTERFACE_CONNECTION_ERROR = "接口连接异常，无法发布";
    public static final String INTERFACE_UNAVAILABLE = "接口状态是关闭或暂时维护，无法访问";
    public static final String NO_PERMISSION = "用户无权限访问";
    public static final String VIEW_ONCE_ONLY = "查看机会仅有一次，无法再次查看";

    public static final String PARAM_EMPTY = "请求参数为空";
    public static final String PARAM_INVALID = "请求参数为空或不合法";
    public static final String PARAM_FORMAT_ERROR = "请求参数不合法";
    public static final String PAGE_SIZE_EXCEEDED = "不支持查询过大单页数据条数";
    public static final String CREDENTIAL_INCOMPLETE = "注册信息为空";
    public static final String LOGIN_CREDENTIAL_EMPTY = "账户名或密码为空";
    public static final String USER_ACCOUNT_LENGTH_INVALID = "账户名长度不合法";
    public static final String CURRENT_PASSWORD_INCORRECT = "当前密码错误";
    public static final String OLD_NEW_PASSWORD_SAME ="新密码不能与原密码相同";
    public static final String PASSWORD_EMPTY = "密码为空";
    public static final String PASSWORD_LENGTH_INVALID = "密码长度不合法";
    public static final String USER_ACCOUNT_FORMAT_INVALID = "账户名包含字符不合法";
    public static final String PASSWORD_FORMAT_INVALID = "密码包含字符不合法";
    public static final String PASSWORD_AND_CODE_DO_NOT_MATCH = "密码与确认码不一致";
    public static final String USER_ACCOUNT_DUPLICATE = "账户名已存在";
    public static final String EMAIL_DUPLICATE = "邮箱已存在";
    public static final String USER_ACCOUNT_OR_PASSWORD_INCORRECT = "用户名或密码错误";
    public static final String USERNAME_EMPTY = "用户名不能为空";

    public static final String FILE_EMPTY = "所要上传的文件为空";
    public static final String FILENAME_EMPTY = "文件名为空";
    public static final String FILE_UPLOAD_ERROR = "文件上传失败";
    public static final String FILE_SIZE_EXCEEDED = "上传文件大小超出限度";
    public static final String REQUEST_PARAM_TOO_LONG = "请求参数过长";
    public static final String INTERFACE_DESC_TOO_LONG = "接口描述过长";
    public static final String QUERY_TIME_EMPTY = "查询时间参数不能为空";
    public static final String TIME_RANGE_INVALID = "开始时间不能晚于结束时间";
    public static final String TIME_EXCEEDS_CURRENT = "结束时间不能超过当前时间";
    public static final String TIME_SPAN_EXCEEDS_LIMIT = "时间跨度不能超过7天";
    public static final String TIME_EXCEEDS_MAX_HISTORY_DAYS = "只能查询最近30天内的数据";
    public static final String AK_SK_BANNED = "密钥已禁用，无法调用接口";
    public static final String AK_SK_EXPIRED = "服务已过期，请续费";

    public static final String EMAIL_FORMAT_INVALID = "邮箱格式不合法";
    public static final String EMAIL_VERIFY_CODE_SEND_FAILED = "邮箱验证码发送失败";
    public static final String VERIFY_CODE_SENT_NO_REPEAT = "验证码已发送，请勿重复请求";
    public static final String VERIFY_CODE_INCORRECT = "验证码不正确";
    public static final String VERIFY_CODE_INVALID = "验证码无效或过期";
    public static final String VERIFICATION_TIMED_OUT = "验证超时，请重新验证邮箱";

    /**
     * article
     */
    public static final String ARTICLE_TITLE_TOO_LONG = "文章标题过长";
    public static final String ARTICLE_SUBTITLE_TOO_LONG = "文章副标题过长";
    public static final String ARTICLE_TOPIC_TOO_LONG = "文章topic过长";
    public static final String ARTICLE_NOT_FOUND = "文章不存在";
    public static final String AUTHOR_NOT_FOUND = "文章作者不存在";
    public static final String NO_AUTH_EDIT_ARTICLE = "无权限更新此文章";

    public static final String INTERFACE_PATH_DUPLICATE = "接口请求路径已存在，不允许重复添加或修改";
    public static final String INTERFACE_STATUS_INVALID = "接口状态值不符合规定值";
    public static final String QUERY_FIELD_INVALID = "查询字段不符合规定值";
    public static final String OBJECT_NOT_FOUND = "找不到操作对象";

    public static final String DATABASE_DELETE_FAILED = "删除失败";
    public static final String DATABASE_INSERT_FAILED = "添加失败";
    public static final String DATABASE_UPDATE_FAILED = "修改失败";

}
