
# --- !Ups

create table book (
  bid                           bigint auto_increment not null,
  isbn                          varchar(255) not null,
  constraint uq_book_isbn unique (isbn),
  constraint pk_book primary key (bid)
);

create table book_entity (
  eid                           bigint auto_increment not null,
  avater_loc                    varchar(1024),
  bid                           bigint not null,
  uid                           bigint not null,
  status                        tinyint(1) default 0 not null,
  possible_next_owner_uid       bigint,
  request_time                  datetime(6),
  constraint pk_book_entity primary key (eid)
);

create table user (
  uid                           bigint auto_increment not null,
  name                          varchar(255) not null,
  email                         varchar(255) not null,
  is_vaild_email                tinyint(1) default 0 not null,
  avater_loc                    varchar(1024),
  psd                           varchar(255) not null,
  dob                           datetime(6) not null,
  dor                           datetime(6) not null,
  post_code                     varchar(255),
  credit                        int default 0 not null,
  rating                        decimal(3,2) default 3 not null,
  rating_confidence             decimal(3,2) default 3 not null,
  total_lent                    bigint default 0 not null,
  total_num_of_praise           bigint default 0 not null,
  in_block                      tinyint(1) default 0 not null,
  recent_latitude               double not null,
  recent_longitude              double not null,
  recent_ipv4                   varchar(255) not null,
  allow_gps                     tinyint(1) default 1 not null,
  last_login_rec                longtext,
  uuid                          varchar(255),
  uuid_valid_until              bigint,
  psw_resetuuid                 varchar(255),
  psw_reset_uuid_valid_until    bigint,
  random_code                   bigint,
  constraint uq_user_email unique (email),
  constraint pk_user primary key (uid)
);

alter table book_entity add constraint fk_book_entity_bid foreign key (bid) references book (bid) on delete restrict on update restrict;
create index ix_book_entity_bid on book_entity (bid);

alter table book_entity add constraint fk_book_entity_uid foreign key (uid) references user (uid) on delete restrict on update restrict;
create index ix_book_entity_uid on book_entity (uid);

alter table book_entity add constraint fk_book_entity_possible_next_owner_uid foreign key (possible_next_owner_uid) references user (uid) on delete restrict on update restrict;
create index ix_book_entity_possible_next_owner_uid on book_entity (possible_next_owner_uid);



