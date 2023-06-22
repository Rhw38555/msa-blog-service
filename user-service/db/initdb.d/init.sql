create table users (
                       id int auto_increment not null comment '사용자 index id',
                       email varchar(50) not null comment '사용자 email',
                       name varchar(50) not null comment '사용자 이름',
                       userId varchar(50) not null comment '사용자 아이디',
                       encryptedPwd TEXT not null comment '사용자 암호화 비밀번호',
                       primary key (id)
) comment '사용자 정보' collate = utf8mb4_bin;
