INSERT INTO `leaf`.`leaf` (`biz_tag`, `max_id`, `step`, `description`, `update_time`)
VALUES ('leaf-segment-hannote-id', 10100, 2000, '小憨书 ID', now());

INSERT INTO `leaf`.`leaf` (`biz_tag`, `max_id`, `step`, `description`, `update_time`)
VALUES ('leaf-segment-user-id', 100, 2000, '用户 ID', now());

CREATE TABLE `leaf`
(
    `biz_tag`     varchar(128) NOT NULL DEFAULT '',
    `max_id`      bigint(20)   NOT NULL DEFAULT '1',
    `step`        int(11)      NOT NULL,
    `description` varchar(256)          DEFAULT NULL,
    `update_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`biz_tag`)
) ENGINE = InnoDB;


