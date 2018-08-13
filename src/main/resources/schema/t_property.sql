create table t_property
(
  id           int auto_increment
  primary key,
  name         varchar(100) not null
  comment '应用名',
  creator      varchar(100) not null
  comment '创建者',
  data         text         null
  comment '数据',
  gmt_create   datetime     not null
  comment '创建时间',
  gmt_modified datetime     not null
  comment '修改时间',
  env varchar(20) not null
  comment '环境',
  sync char not null
  comment '是否已同步'
) engine = InnoDB default charset = UTF8;

alter table t_property add unique key name_env_unique(name,env);