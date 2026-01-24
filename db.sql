create table post
(
    id          bigint auto_increment comment 'id'
        primary key,
    title       varchar(512)                       null comment '标题',
    zone        varchar(255)                       null comment '帖子分区',
    content     text                               null comment '内容',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    view_num    int      default 0                 null comment '观看数',
    thumb_num   int      default 0                 null comment '点赞数',
    favour_num  int      default 0                 null comment '收藏数',
    user_id     bigint                             null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updater     bigint                             null comment '更新人',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 null comment '是否删除'
)
    comment '帖子' collate = utf8mb4_unicode_ci
                   row_format = DYNAMIC;

create index idx_user_id
    on post (creator);

create table post_comment
(
    id          bigint   not null comment '主键'
        primary key,
    parent_id   bigint   null comment '父id',
    post_id     bigint   null comment '帖子id',
    content     text     null comment '内容',
    user_id   bigint   null comment '作者id',
    create_time datetime null comment '创建时间',
    is_delete    tinyint  null comment '逻辑删除（0-未删除，1-已删除）'
)
    comment '帖子评论' collate = utf8mb4_general_ci
                       row_format = DYNAMIC;

create table post_favour
(
    id          bigint auto_increment comment 'id'
        primary key,
    post_id     bigint                             not null comment '帖子 id',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '帖子收藏' row_format = DYNAMIC;

create index idx_post_id
    on post_favour (post_id);

create index idx_user_id
    on post_favour (user_id);

create table post_thumb
(
    id          bigint auto_increment comment 'id'
        primary key,
    post_id     bigint                             not null comment '帖子 id',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '帖子点赞' row_format = DYNAMIC;

create index idx_post_id
    on post_thumb (post_id);

create index idx_user_id
    on post_thumb (user_id);

create table question
(
    id          bigint auto_increment comment 'id'
        primary key,
    title       varchar(512)                       null comment '标题',
    content     text                               null comment '内容',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    answer      text                               null comment '题目答案',
    submit_num   int      default 0                 not null comment '题目提交数',
    accepted_num int      default 0                 not null comment '题目通过数',
    judge_case   text                               null comment '判题用例（json 数组）',
    judge_config text                               null comment '判题配置（json 对象）',
    thumb_num    int      default 0                 not null comment '点赞数',
    favour_num   int      default 0                 not null comment '收藏数',
    user_id      bigint                             not null comment '创建用户 id',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除'
)
    comment '题目' collate = utf8mb4_unicode_ci;

create index idx_user_id
    on question (user_id);

create table question_comment
(
    id          bigint        not null comment '主键'
        primary key,
    tags        varchar(1024) null comment '标签',
    parent_id   bigint        null comment '父id',
    question_id bigint        null comment '题目id',
    content     text          null comment '内容',
    author_id   bigint        null comment '作者id',
    create_time datetime      null comment '创建时间',
    del_flag    tinyint       null comment '逻辑删除（0-未删除，1-已删除）'
)
    comment '题目评论' collate = utf8mb4_general_ci
                       row_format = DYNAMIC;

create table question_submit
(
    id         bigint auto_increment comment 'id'
        primary key,
    language   varchar(128)                       not null comment '编程语言',
    code       text                               not null comment '用户代码',
    judge_info  text                               null comment '判题信息（json 对象）',
    status     int      default 0                 not null comment '判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）',
    question_id bigint                             not null comment '题目 id',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除'
)
    comment '题目提交';

create index idx_question_id
    on question_submit (question_id);

create index idx_user_id
    on question_submit (user_id);

create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    user_account  varchar(256)                           not null comment '账号',
    user_password varchar(512)                           not null comment '密码',
    union_id      varchar(256)                           null comment '微信开放平台id',
    mp_open_id    varchar(256)                           null comment '公众号openId',
    user_name     varchar(256)                           null comment '用户昵称',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_union_id
    on user (union_id);

-- 为Python爬虫入门教程帖子(2003329711446446084)添加评论数据
INSERT INTO post_comment (id, parent_id, post_id, content, user_id, create_time, is_delete) VALUES
-- 顶级评论
(3003329711446446081, NULL, 2003329711446446084, '这篇教程写得很好，很适合Python爬虫入门学习！', 2012025463920201729, '2026-01-24 12:05:00', 0),
(3003329711446446082, NULL, 2003329711446446084, 'requests库和BeautifulSoup的组合确实很经典，谢谢分享！', 2008905951830183938, '2026-01-24 12:10:00', 0),
(3003329711446446083, NULL, 2003329711446446084, '能不能加一些反爬虫的处理方法？比如User-Agent设置等', 2012025463920201730, '2026-01-24 12:15:00', 0),
(3003329711446446084, NULL, 2003329711446446084, '跟着教程实践了一遍，成功爬取了数据，太棒了！', 2012025463920201731, '2026-01-24 12:20:00', 0),
-- 子评论（回复顶级评论）
(3003329711446446085, 3003329711446446083, 2003329711446446084, '教程后面有提到User-Agent设置，你可以看看后面的章节', 2008905951830183938, '2026-01-24 12:25:00', 0),
(3003329711446446086, 3003329711446446083, 2003329711446446084, '还可以设置延时请求，避免被反爬虫检测', 2012025463920201732, '2026-01-24 12:30:00', 0),
(3003329711446446087, 3003329711446446084, 2003329711446446084, '你爬取了什么网站？能分享一下经验吗？', 2012025463920201733, '2026-01-24 12:35:00', 0),
(3003329711446446088, 3003329711446446087, 2003329711446446084, '我爬取了豆瓣电影top250，很有成就感！', 2012025463920201731, '2026-01-24 12:40:00', 0);