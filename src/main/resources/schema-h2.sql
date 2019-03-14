create table user
(
  id int primary key,
  name varchar(100) not null,
  password varchar(100) not null
);

create table student
(
  id int primary key,
  name varchar(100) not null,
  dob date
);