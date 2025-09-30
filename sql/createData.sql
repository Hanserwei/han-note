INSERT INTO `t_permission` (`id`, `parent_id`, `name`, `type`, `menu_url`, `menu_icon`, `sort`, `permission_key`,
                            `status`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 0, '发布笔记', 3, '', '', 1, 'app:note:publish', 0, now(), now(), b'0');

INSERT INTO `t_permission` (`id`, `parent_id`, `name`, `type`, `menu_url`, `menu_icon`, `sort`, `permission_key`,
                            `status`, `create_time`, `update_time`, `is_deleted`)
VALUES (2, 0, '发布评论', 3, '', '', 2, 'app:comment:publish', 0, now(), now(), b'0');

INSERT INTO `t_role` (`id`, `role_name`, `role_key`, `status`, `sort`, `remark`, `create_time`, `update_time`,
                      `is_deleted`)
VALUES (1, '普通用户', 'common_user', 0, 1, '', now(), now(), b'0');

INSERT INTO `t_role_permission_rel` (`id`, `role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 1, 1, now(), now(), b'0');

INSERT INTO `t_role_permission_rel` (`id`, `role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`)
VALUES (2, 1, 2, now(), now(), b'0');
