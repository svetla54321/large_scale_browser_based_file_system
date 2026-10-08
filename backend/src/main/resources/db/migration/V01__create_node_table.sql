create table node (
    id            bigint generated always as identity primary key,
    version       bigint      not null default 0,
    parent_id     bigint,
    type          text        not null,
    name          text        collate "und-x-icu" not null,
    name_key      text        collate "C" not null,
    ancestor_path text        collate "C" not null,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    constraint fk_node_parent foreign key (parent_id) references node (id) on delete cascade,
    constraint uq_node_parent_name unique (parent_id, name_key),
    constraint ck_node_type check (type in ('FOLDER', 'FILE')),
    constraint ck_node_root_is_folder check (parent_id is not null or type = 'FOLDER'),
    constraint ck_node_name_length check (char_length(name) between 1 and 255)
);

create unique index uq_node_single_root on node ((parent_id is null)) where parent_id is null;

create index ix_node_children on node (parent_id, type, name);
