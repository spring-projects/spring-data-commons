DROP TABLE IF EXISTS T_AUDITS;

create table T_AUDITS (
	id integer not null primary key,
	operation varchar(20),
	name varchar(80),
	audit_date timestamp
);
