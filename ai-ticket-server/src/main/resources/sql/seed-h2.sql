-- H2 数据库初始化数据

-- 管理员
INSERT INTO sys_admin (username, password_hash, real_name) VALUES
('admin', 'admin123', '系统管理员');

-- 客服分组
INSERT INTO agent_group (name, scene, leader_agent_id) VALUES
('校园报修组', '校园报修', 1),
('售后客服组', '产品售后', NULL),
('投诉建议组', '投诉建议', NULL),
('业务咨询组', 'AI 咨询', NULL);

-- 客服坐席
INSERT INTO sys_agent (username, password_hash, real_name, online_status, group_id, role, status) VALUES
('agent001', 'agent123', '张工', 'ONLINE', 1, 'LEADER', 'ACTIVE'),
('agent002', 'agent123', '李工', 'ONLINE', 1, 'AGENT', 'ACTIVE'),
('agent003', 'agent123', '王工', 'OFFLINE', 2, 'AGENT', 'ACTIVE'),
('agent004', 'agent123', '赵工', 'ONLINE', 2, 'AGENT', 'ACTIVE');

-- 普通用户
INSERT INTO sys_user (username, password_hash, phone, email, status) VALUES
('user001', 'user123', '13800000001', 'user1@example.com', 'ACTIVE'),
('user002', 'user123', '13800000002', 'user2@example.com', 'ACTIVE'),
('user003', 'user123', '13800000003', 'user3@example.com', 'ACTIVE');

-- FAQ 分类
INSERT INTO faq_category (name, parent_id, sort_order) VALUES
('校园生活', NULL, 1),
('教学教务', NULL, 2),
('图书资源', NULL, 3),
('网络服务', NULL, 4);

-- FAQ 问答
INSERT INTO faq_entry (category_id, question, answer, keywords, enabled) VALUES
(1, '宿舍水管漏水怎么办？', '您好，宿舍水管漏水请立即拨打后勤报修电话 010-12345678，或在本平台提交工单，维修人员会在 2 小时内上门处理。请在报修时注明宿舍楼号、房间号和漏水位置。', '水管,漏水,维修,后勤', 1),
(1, '食堂开放时间是几点？', '学校食堂开放时间如下：早餐 6:30-9:00，午餐 11:00-13:30，晚餐 17:00-19:30。周末及节假日营业时间可能略有调整，请以食堂通知为准。', '食堂,开放时间,吃饭', 1),
(2, '如何查询考试成绩？', '考试成绩可通过教务系统查询：登录学校官网 → 进入教务系统 → 选择"成绩查询"。一般在考试结束后 1-2 周内公布成绩。如有疑问可联系教务处。', '成绩,考试,教务系统', 1),
(4, '校园网怎么连接？', '校园网连接方式：1. 搜索 WiFi 信号 "CampusNet"；2. 使用学号和密码登录；3. 首次使用需要绑定手机号。如有问题请联系网络中心 010-87654321。', '校园网,WiFi,网络,连接', 1),
(3, '图书馆怎么借书？', '借书流程：1. 携带校园卡到图书馆；2. 在书架找到所需书籍；3. 到一楼自助借还机或服务台办理借阅。本科生最多可借 20 本，借期 30 天，可续借一次。', '图书馆,借书,图书', 1),
(1, '快递在哪里取？', '学校快递驿站位于学生活动中心一层，提供主流快递公司包裹代收服务。取件时请携带校园卡或手机取件码。营业时间：8:00-21:00。', '快递,取件,驿站', 1);

-- 示例工单
INSERT INTO ticket (title, description, category, department, priority, status, source, user_id, assignee_agent_id, timeout_at, created_at) VALUES
('宿舍水龙头漏水', '3号楼302室卫生间水龙头漏水，水流量很大，需要尽快维修。', '校园报修', '后勤保障部', 'HIGH', 'COMPLETED', 'USER_FORM', 1, 1, DATEADD('HOUR', -20, CURRENT_TIMESTAMP), DATEADD('HOUR', -24, CURRENT_TIMESTAMP)),
('校园网连不上', '宿舍区 WiFi 信号满格但无法上网，已经重启过路由器还是不行。', '业务咨询', '信息技术部', 'NORMAL', 'PROCESSING', 'USER_FORM', 2, 4, DATEADD('HOUR', 12, CURRENT_TIMESTAMP), DATEADD('HOUR', -6, CURRENT_TIMESTAMP)),
('食堂菜品建议', '建议增加素食窗口，现在素菜选择太少了。', '投诉建议', '后勤保障部', 'LOW', 'ASSIGNED', 'USER_FORM', 3, NULL, DATEADD('HOUR', 20, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
('图书馆借书逾期', '忘了还书，已经逾期3天，会有什么处罚吗？', '业务咨询', '图书馆', 'NORMAL', 'FOLLOWING', 'AI_CHAT', 1, 2, DATEADD('HOUR', 8, CURRENT_TIMESTAMP), DATEADD('HOUR', -10, CURRENT_TIMESTAMP)),
('空调遥控器损坏', '宿舍空调遥控器按键失灵，无法调节温度。', '校园报修', '后勤保障部', 'LOW', 'ARCHIVED', 'USER_FORM', 2, 1, DATEADD('HOUR', -100, CURRENT_TIMESTAMP), DATEADD('HOUR', -120, CURRENT_TIMESTAMP));

-- 工单事件
INSERT INTO ticket_event (ticket_id, event_type, from_status, to_status, operator_type, operator_id, remark) VALUES
(1, 'CREATE', NULL, 'ASSIGNED', 'USER', 1, '用户提交工单'),
(1, 'ACCEPT', 'ASSIGNED', 'ACCEPTED', 'AGENT', 1, '客服接单'),
(1, 'PROCESS', 'ACCEPTED', 'PROCESSING', 'AGENT', 1, '维修人员已出发'),
(1, 'COMPLETE', 'PROCESSING', 'COMPLETED', 'AGENT', 1, '已完成维修'),
(2, 'CREATE', NULL, 'ASSIGNED', 'USER', 2, '用户提交工单'),
(2, 'ACCEPT', 'ASSIGNED', 'ACCEPTED', 'AGENT', 4, '客服接单'),
(2, 'PROCESS', 'ACCEPTED', 'PROCESSING', 'AGENT', 4, '正在排查网络问题'),
(5, 'CREATE', NULL, 'ASSIGNED', 'USER', 2, '用户提交工单'),
(5, 'ACCEPT', 'ASSIGNED', 'ACCEPTED', 'AGENT', 1, '客服接单'),
(5, 'PROCESS', 'ACCEPTED', 'PROCESSING', 'AGENT', 1, '已安排更换遥控器'),
(5, 'COMPLETE', 'PROCESSING', 'COMPLETED', 'AGENT', 1, '遥控器已更换'),
(5, 'ARCHIVE', 'COMPLETED', 'ARCHIVED', 'ADMIN', 1, '工单已归档');

-- 满意度评价
INSERT INTO satisfaction (ticket_id, user_id, score, comment, created_at) VALUES
(1, 1, 5, '维修很及时，师傅态度也很好', DATEADD('HOUR', -18, CURRENT_TIMESTAMP));
