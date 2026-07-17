INSERT INTO sys_user (id, username, password_hash, phone, email, status) VALUES
  (1, 'demo', 'demo123456', '13800000001', 'demo@example.com', 'ACTIVE'),
  (2, 'student01', 'demo123456', '13800000002', 'student01@example.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE username = VALUES(username), password_hash = VALUES(password_hash), phone = VALUES(phone), email = VALUES(email), status = VALUES(status);

INSERT INTO agent_group (id, name, scene) VALUES
  (1, '校园后勤组', 'CAMPUS_REPAIR'),
  (2, '电商售后组', 'ECOMMERCE_AFTERSALE'),
  (3, '门店投诉组', 'STORE_COMPLAINT')
ON DUPLICATE KEY UPDATE name = VALUES(name), scene = VALUES(scene);

INSERT INTO sys_agent (id, username, password_hash, real_name, online_status, group_id, role, status) VALUES
  (1, 'agent', 'agent123456', '演示客服', 'ONLINE', 1, 'AGENT', 'ACTIVE'),
  (2, 'agent2', 'agent123456', '售后客服', 'OFFLINE', 2, 'AGENT', 'ACTIVE')
ON DUPLICATE KEY UPDATE username = VALUES(username), password_hash = VALUES(password_hash), real_name = VALUES(real_name), online_status = VALUES(online_status), group_id = VALUES(group_id), role = VALUES(role), status = VALUES(status);

INSERT INTO sys_admin (id, username, password_hash, real_name, status) VALUES
  (1, 'admin', 'admin123456', '演示管理员', 'ACTIVE')
ON DUPLICATE KEY UPDATE username = VALUES(username), password_hash = VALUES(password_hash), real_name = VALUES(real_name), status = VALUES(status);

INSERT INTO faq_category (id, name, sort_order) VALUES
  (1, '校园报修', 10),
  (2, '产品售后', 20),
  (3, '投诉建议', 30),
  (4, '业务咨询', 40)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

INSERT INTO faq_entry (id, category_id, question, answer, keywords, enabled) VALUES
  (1, 1, '如何提交宿舍报修？', '进入用户端工单创建页面，选择校园报修分类并填写问题描述后提交。', '宿舍,报修,后勤,水管,空调', 1),
  (2, 2, '商品售后如何联系人工？', '在 AI 咨询页面点击转人工，系统会携带历史会话创建售后工单。', '售后,人工,转接,退款,订单', 1),
  (3, 3, '投诉建议多久处理？', '普通投诉建议将在 24 小时内响应，高优先级问题会触发超时预警。', '投诉,建议,处理时效', 1),
  (4, 1, '宿舍水管漏水怎么办？', '请先关闭附近阀门并拍照留存，然后提交校园报修工单；紧急情况可选择高优先级。', '宿舍,水管,漏水,紧急', 1),
  (5, 2, '退款一般多久到账？', '退款审核通过后通常 1-3 个工作日原路退回，具体到账时间以支付渠道为准。', '退款,到账,售后', 1),
  (6, 4, 'AI 客服可以处理哪些问题？', 'AI 客服可回答 FAQ、收集问题信息，并在需要时转人工生成工单。', 'AI,客服,咨询,工单', 1)
ON DUPLICATE KEY UPDATE category_id = VALUES(category_id), question = VALUES(question), answer = VALUES(answer), keywords = VALUES(keywords), enabled = VALUES(enabled);

INSERT INTO ticket (id, title, description, category, department, priority, status, source, user_id, assignee_agent_id, timeout_at) VALUES
  (1, '宿舍水管漏水', '3 号楼 502 洗手池下方持续漏水，需要维修。', '校园报修', '后勤维修', 'HIGH', 'ASSIGNED', 'USER_FORM', 1, NULL, DATE_ADD(NOW(), INTERVAL 12 HOUR)),
  (2, '订单退款咨询', '订单 A20260717001 申请退款后未到账。', '产品售后', '售后服务', 'NORMAL', 'ACCEPTED', 'USER_FORM', 1, 1, DATE_ADD(NOW(), INTERVAL 24 HOUR))
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), category = VALUES(category), department = VALUES(department), priority = VALUES(priority), status = VALUES(status), source = VALUES(source), user_id = VALUES(user_id), assignee_agent_id = VALUES(assignee_agent_id), timeout_at = VALUES(timeout_at);

INSERT INTO ticket_event (id, ticket_id, event_type, from_status, to_status, operator_type, operator_id, remark) VALUES
  (1, 1, 'CREATE', NULL, 'ASSIGNED', 'USER', 1, '用户提交工单，进入待接单池'),
  (2, 2, 'CREATE', NULL, 'ASSIGNED', 'USER', 1, '用户提交工单，进入待接单池'),
  (3, 2, 'ACCEPT', 'ASSIGNED', 'ACCEPTED', 'AGENT', 1, '演示客服接单')
ON DUPLICATE KEY UPDATE ticket_id = VALUES(ticket_id), event_type = VALUES(event_type), from_status = VALUES(from_status), to_status = VALUES(to_status), operator_type = VALUES(operator_type), operator_id = VALUES(operator_id), remark = VALUES(remark);
