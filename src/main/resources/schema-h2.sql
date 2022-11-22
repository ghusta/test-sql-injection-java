create table "user"
(
  id integer,
  name varchar(100) not null,
  password varchar(100) not null,
  PRIMARY KEY (id)
);

create table student
(
  id integer,
  name varchar(100) not null,
  dob date,
  PRIMARY KEY (id)
);