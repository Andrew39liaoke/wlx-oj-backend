create table class
(
    id              bigint auto_increment comment '班级id'
        primary key,
    teacher_id      bigint                             not null comment '教师id',
    name            varchar(255)                       not null comment '班级名称',
    invitation_code varchar(255)                       not null comment '邀请码',
    join_number     int      default 0                 null comment '加入人数',
    create_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    file_info_id    bigint                             null
)
    comment '班级';

create index idx_invitation_code
    on class (invitation_code);

create index idx_teacher_id
    on class (teacher_id);

create table class_chat_message
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    class_id     bigint                             not null comment '班级ID',
    sender_id    bigint                             not null comment '发送者用户ID',
    content      text                               not null comment '聊天内容',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '发送时间',
    is_delete    tinyint  default 0                 not null comment '是否删除 (0-未删除, 1-已删除)',
    message_type tinyint  default 0                 null comment '消息类型：0-文本，1-图片',
    image_url    varchar(512)                       null comment '图片URL（OSS地址）'
)
    comment '班级实时聊天记录表';

create index idx_class_id_create_time
    on class_chat_message (class_id, create_time);

create table class_knowledge
(
    id           bigint auto_increment comment '主键'
        primary key,
    class_id     bigint                             not null comment '班级ID',
    file_info_id bigint                             not null comment '文件信息ID，关联file_info表',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除(0-未删除, 1-已删除)'
)
    comment '班级知识库表';

create index idx_classId
    on class_knowledge (class_id);

create index idx_fileInfoId
    on class_knowledge (file_info_id);

create table class_problem
(
    id         bigint auto_increment comment '班级题目id'
        primary key,
    problem_id bigint not null comment '题库id',
    class_id   bigint not null comment '班级id'
)
    comment '班级题目关联表';

create index idx_class_id
    on class_problem (class_id);

create index idx_problem_id
    on class_problem (problem_id);

create table exam_answer_detail
(
    id             bigint auto_increment comment '主键'
        primary key,
    record_id      bigint                             not null comment '答题记录ID',
    question_id    bigint                             not null comment '考试题目ID',
    user_answer    varchar(50)                        null comment '用户答案',
    is_correct     tinyint                            null comment '是否正确：0-错误，1-正确，2-部分正确',
    score_obtained int      default 0                 not null comment '该题实际得分',
    score_full     int                                not null comment '该题满分',
    question_order int                                not null comment '题目顺序',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '答题详情表';

create index idx_question_id
    on exam_answer_detail (question_id);

create index idx_record_id
    on exam_answer_detail (record_id);

create table exam_answer_record
(
    id            bigint auto_increment comment '答题记录ID'
        primary key,
    paper_id      bigint                             not null comment '试卷ID',
    user_id       bigint                             not null comment '答题用户ID',
    total_score   int                                null comment '总得分',
    correct_count int                                null comment '正确题目数',
    total_count   int      default 20                not null comment '总题目数',
    accuracy_rate double                             null comment '正确率（0~1）',
    start_time    datetime                           not null comment '开始答题时间',
    submit_time   datetime                           null comment '提交时间',
    time_spent    int                                null comment '用时（秒）',
    status        tinyint  default 0                 not null comment '状态：0-答题中，1-已提交，2-已批改',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint  default 0                 not null comment '是否删除',
    constraint uk_paper_user
        unique (paper_id, user_id)
)
    comment '用户答题记录表';

create index idx_paper_id
    on exam_answer_record (paper_id);

create index idx_user_id
    on exam_answer_record (user_id);

create table exam_paper
(
    id              bigint auto_increment comment '试卷ID'
        primary key,
    title           varchar(255)                       not null comment '试卷标题',
    description     text                               null comment '试卷描述',
    class_id        bigint                             not null comment '所属班级ID',
    total_score     int      default 100               not null comment '试卷总分',
    question_count  int      default 20                not null comment '题目数量',
    time_limit      int                                null comment '考试时限（分钟），NULL表示不限时',
    single_count    int      default 0                 not null comment '单选题数量',
    multi_count     int      default 0                 not null comment '多选题数量',
    difficulty_dist json                               null comment '难度分布配置',
    knowledge_ids   varchar(500)                       null comment '考察知识点ID列表',
    status          tinyint  default 0                 not null comment '状态：0-草稿，1-已发布，2-已结束',
    start_time      datetime                           null comment '考试开始时间',
    end_time        datetime                           null comment '考试结束时间',
    creator_id      bigint                             not null comment '创建者ID',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete       tinyint  default 0                 not null comment '是否删除'
)
    comment '考试试卷表';

create index idx_class_id
    on exam_paper (class_id);

create index idx_creator_id
    on exam_paper (creator_id);

create index idx_status
    on exam_paper (status);

create table exam_paper_question
(
    id             bigint auto_increment comment '主键'
        primary key,
    paper_id       bigint        not null comment '试卷ID',
    question_id    bigint        not null comment '考试题目ID',
    question_order int           not null comment '题目在试卷中的顺序',
    score          int default 5 not null comment '该题在本试卷中的分值',
    constraint uk_paper_question
        unique (paper_id, question_id)
)
    comment '试卷题目关联表';

create index idx_paper_id
    on exam_paper_question (paper_id);

create table exam_question
(
    id             bigint auto_increment comment '题目ID'
        primary key,
    title          text                               not null comment '题目内容（支持富文本/Markdown）',
    question_type  tinyint                            not null comment '题型：1-单选题，2-多选题',
    options        json                               not null comment '选项列表，JSON数组',
    correct_answer varchar(50)                        not null comment '正确答案，单选如A，多选如A,B,D',
    score          int      default 5                 not null comment '题目分值',
    difficulty     tinyint  default 2                 not null comment '难度等级：1-简单，2-中等，3-困难',
    knowledge_ids  varchar(500)                       null comment '关联知识点ID列表，逗号分隔',
    tags           varchar(1024)                      null comment '标签列表（JSON数组）',
    analysis       text                               null comment '题目解析',
    user_id        bigint                             not null comment '创建者用户ID',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint  default 0                 not null comment '是否删除'
)
    comment '考试题目表（选择题）';

create index idx_difficulty
    on exam_question (difficulty);

create index idx_question_type
    on exam_question (question_type);

create index idx_user_id
    on exam_question (user_id);

create table file_info
(
    id          bigint auto_increment comment '主键'
        primary key,
    file_name   varchar(255)                       not null comment '原始文件名',
    file_path   varchar(500)                       not null comment '文件存储路径',
    file_size   bigint                             not null comment '文件大小(字节)',
    file_type   varchar(500)                       not null comment '文件类型',
    file_url    varchar(500)                       not null comment '文件访问URL',
    user_id     bigint                             not null comment '上传用户ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    is_delete   tinyint  default 0                 null comment '是否删除'
)
    comment '文件信息表';

create index idx_create_time
    on file_info (create_time);

create index idx_user_id
    on file_info (user_id);

create table knowledge_ability
(
    id             bigint auto_increment comment '主键'
        primary key,
    user_id        bigint                             not null comment '用户ID',
    knowledge_id   bigint                             not null comment '知识点ID',
    record_id      bigint                             not null comment '关联的答题记录ID',
    total_score    int      default 0                 not null comment '该知识点下题目总满分',
    obtained_score int      default 0                 not null comment '该知识点下实际得分',
    correct_count  int      default 0                 not null comment '正确题目数',
    total_count    int      default 0                 not null comment '总题目数',
    mastery_rate   double   default 0                 not null comment '掌握度（0~1）',
    mastery_level  tinyint  default 0                 not null comment '掌握等级：1-薄弱，2-一般，3-良好',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '知识点能力记录表';

create index idx_knowledge_id
    on knowledge_ability (knowledge_id);

create index idx_record_id
    on knowledge_ability (record_id);

create index idx_user_id
    on knowledge_ability (user_id);

create index idx_user_knowledge
    on knowledge_ability (user_id, knowledge_id);

create table knowledge_dependency
(
    id                bigint auto_increment comment '主键'
        primary key,
    from_knowledge_id bigint                             not null comment '前置知识点ID（被依赖方）',
    to_knowledge_id   bigint                             not null comment '后续知识点ID（依赖方）',
    dependency_type   tinyint  default 1                 not null comment '依赖类型：1-前置依赖，2-关联关系',
    weight            double   default 1                 not null comment '关系权重（0~1）',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_from_to
        unique (from_knowledge_id, to_knowledge_id)
)
    comment '知识点依赖关系表（知识图谱）';

create index idx_to
    on knowledge_dependency (to_knowledge_id);

create table knowledge_point
(
    id          bigint auto_increment comment '知识点ID'
        primary key,
    name        varchar(255)                       not null comment '知识点名称',
    description text                               null comment '知识点描述',
    parent_id   bigint                             null comment '父知识点ID（支持层级结构）',
    class_id    bigint                             null comment '所属班级ID（NULL表示全局知识点）',
    sort_order  int      default 0                 not null comment '排序顺序',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除'
)
    comment '知识点表';

create index idx_class_id
    on knowledge_point (class_id);

create index idx_parent_id
    on knowledge_point (parent_id);

create table live_room
(
    id           bigint auto_increment
        primary key,
    class_id     bigint                                 not null comment '班级ID',
    teacher_id   bigint                                 not null comment '教师(主播)ID',
    title        varchar(200) default '班级直播'        null comment '直播标题',
    status       tinyint      default 0                 not null comment '0-未开始 1-直播中 2-已结束',
    stream_id    varchar(100)                           null comment 'SRS 流ID',
    start_time   datetime                               null comment '开始时间',
    end_time     datetime                               null comment '结束时间',
    viewer_count int          default 0                 not null comment '观看人数',
    create_time  datetime     default CURRENT_TIMESTAMP not null,
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    is_delete    tinyint      default 0                 not null
)
    comment '直播间表';

create index idx_classId
    on live_room (class_id);

create index idx_status
    on live_room (status);

create table permission
(
    id        int unsigned auto_increment
        primary key,
    name      varchar(255)                 not null comment '名称',
    url       varchar(255)                 not null comment '接口路径',
    method    tinyint unsigned default '0' not null comment '请求方式（0-get；1-post；2-put；3-delete）',
    service   varchar(255)     default ''  not null comment '服务名',
    parent_id int              default 0   not null comment '父级权限id'
)
    comment '系统权限表' collate = utf8mb4_general_ci
                         row_format = DYNAMIC;

create table post
(
    id          bigint auto_increment comment 'id'
        primary key,
    title       varchar(512)                       null comment '标题',
    zone        varchar(255)                       null comment '帖子分区',
    content     longtext                           null comment '内容',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    view_num    int      default 0                 null comment '观看数',
    thumb_num   int      default 0                 null comment '点赞数',
    favour_num  int      default 0                 null comment '收藏数',
    user_id     bigint                             null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updater     bigint                             null comment '更新人',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 null comment '是否删除',
    cover       varchar(1024)                      null
)
    comment '帖子' collate = utf8mb4_unicode_ci
                   row_format = DYNAMIC;

create index idx_user_id
    on post (user_id);

create table post_comment
(
    id          bigint   not null comment '主键'
        primary key,
    parent_id   bigint   null comment '父id',
    post_id     bigint   null comment '帖子id',
    content     text     null comment '内容',
    user_id     bigint   null comment '作者id',
    create_time datetime null comment '创建时间',
    is_delete   tinyint  null comment '逻辑删除（0-未删除，1-已删除）'
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

create index idx_postId
    on post_favour (post_id);

create index idx_userId
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

create index idx_postId
    on post_thumb (post_id);

create index idx_userId
    on post_thumb (user_id);

create table practice_recommendation
(
    id               bigint auto_increment comment '主键'
        primary key,
    user_id          bigint                             not null comment '用户ID',
    record_id        bigint                             not null comment '关联的答题记录ID',
    knowledge_id     bigint                             not null comment '薄弱知识点ID',
    question_id      bigint                             not null comment '推荐题目ID',
    recommend_type   tinyint                            not null comment '推荐类型：1-规则，2-知识图谱，3-相似度',
    recommend_reason longtext                           null comment '推荐理由',
    priority         int      default 0                 not null comment '推荐优先级',
    difficulty       tinyint                            not null comment '推荐题目难度',
    is_practiced     tinyint  default 0                 not null comment '是否已练习：0-未练习，1-已练习',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '练习推荐记录表';

create index idx_knowledge_id
    on practice_recommendation (knowledge_id);

create index idx_record_id
    on practice_recommendation (record_id);

create index idx_user_id
    on practice_recommendation (user_id);

create table question
(
    id           bigint auto_increment comment 'id'
        primary key,
    title        varchar(512)                       null comment '标题',
    content      text                               null comment '内容',
    tags         varchar(1024)                      null comment '标签列表（json 数组）',
    answer       text                               null comment '题目答案',
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
    is_delete   tinyint       null comment '逻辑删除（0-未删除，1-已删除）'
)
    comment '题目评论' collate = utf8mb4_general_ci
                       row_format = DYNAMIC;

create table question_favour
(
    id          bigint auto_increment comment 'id'
        primary key,
    question_id bigint                             not null comment '题目 id',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '题目收藏' row_format = DYNAMIC;

create index idx_questionId
    on question_favour (question_id);

create index idx_userId
    on question_favour (user_id);

create table question_submit
(
    id               bigint auto_increment comment 'id'
        primary key,
    language         varchar(128)                       not null comment '编程语言',
    code             text                               not null comment '用户代码',
    judge_info       text                               null comment '判题信息（json 对象）',
    status           int      default 0                 not null comment '判题状态（0 - 待判题、1 - 判题中、2 - 执行通过、3 - 错误解答、4 - 编译出错、5 - 超出内存限制、6 - 超出时间限制、7 - 展示错误、8 - 超出输出限制、9 - 危险操作、10 - 执行出错、11 - 内部出错）',
    question_id      bigint                             not null comment '题目 id',
    user_id          bigint                             not null comment '创建用户 id',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete        tinyint  default 0                 not null comment '是否删除',
    pass_case_count  int      default 0                 not null comment '通过测试用例数目',
    total_case_count int      default 0                 not null comment '总测试用例数目',
    pass_rate        double   default 0                 not null comment '通过率',
    score            double   default 0                 not null comment '得分'
)
    comment '题目提交';

create index idx_question_id
    on question_submit (question_id);

create index idx_user_id
    on question_submit (user_id);

create table question_thumb
(
    id          bigint auto_increment comment 'id'
        primary key,
    question_id bigint                             not null comment '题目 id',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '题目点赞' row_format = DYNAMIC;

create index idx_questionId
    on question_thumb (question_id);

create index idx_userId
    on question_thumb (user_id);

create table role
(
    id    int unsigned auto_increment
        primary key,
    name  varchar(50) default '' not null comment '名称',
    value varchar(255)           null comment '备注'
)
    comment '系统角色表' collate = utf8mb4_general_ci
                         row_format = DYNAMIC;

create table role_permission
(
    id            int unsigned auto_increment
        primary key,
    role_id       int unsigned not null comment '角色id',
    permission_id int unsigned not null comment '权限id'
)
    comment '角色-权限关联表' collate = utf8mb4_general_ci
                              row_format = DYNAMIC;

create table student_class
(
    id          bigint auto_increment comment '主键id'
        primary key,
    student_id  bigint                             not null comment '学生id',
    class_id    bigint                             not null comment '班级id',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_student_class
        unique (student_id, class_id)
)
    comment '学生班级关联表';

create index idx_class_id
    on student_class (class_id);

create index idx_student_id
    on student_class (student_id);

create table user
(
    id            bigint auto_increment comment 'id'
        primary key,
    user_name     varchar(256)                       not null comment '账号',
    user_password varchar(512)                       not null comment '密码',
    union_id      varchar(256)                       null comment '微信开放平台id',
    mp_open_id    varchar(256)                       null comment '公众号openId',
    nick_name     varchar(256)                       null comment '用户昵称',
    user_avatar   varchar(1024)                      null comment '用户头像',
    user_profile  varchar(512)                       null comment '用户简介',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint  default 0                 not null comment '是否删除'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create table user_recommendation
(
    id             bigint auto_increment comment '主键'
        primary key,
    user_id        bigint                                 not null comment '用户id',
    recommend_type int          default 1                 not null comment '推荐类型（1-题目，2-帖子）',
    item_id        bigint                                 not null comment '推荐物品id',
    score          double       default 0                 not null comment '推荐得分',
    reason         varchar(512) default ''                null comment '推荐理由',
    create_time    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户推荐结果' collate = utf8mb4_unicode_ci;

create index idx_userId
    on user_recommendation (user_id);

create index idx_userId_type
    on user_recommendation (user_id, recommend_type);

create table user_role
(
    id      bigint auto_increment
        primary key,
    role_id bigint not null comment '角色id，数据来源于role表的主键',
    user_id bigint not null comment '用户id，数据来源于user表的主键'
)
    comment '用户-角色关系表表' collate = utf8mb4_general_ci
                                row_format = DYNAMIC;

