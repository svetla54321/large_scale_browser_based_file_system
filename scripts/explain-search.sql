select count(*) as rows, count(*) filter (where type = 'FILE') as files from node;

select pg_size_pretty(pg_relation_size('ix_node_file_search')) as file_search_index,
       pg_size_pretty(pg_relation_size('ix_node_children')) as children_index;

select ancestor_path || id || '/' as subtree_from, ancestor_path || id || '0' as subtree_to
from node
where name = 'team-042'
\gset

select id as project_id from node where name = 'project-042' and ancestor_path = :'subtree_from' \gset

select encode(convert_to('rep' || chr(1114111), 'UTF8'), 'hex') as suggestion_upper_bound_hex;

explain (analyze, buffers)
select id, name, parent_id, ancestor_path
from node
where type = 'FILE'
  and name_key >= 'rep'
  and name_key < 'rep' || chr(1114111)
order by name_key, ancestor_path
limit 10;

explain (analyze, buffers)
select id, name, parent_id, ancestor_path
from node
where type = 'FILE'
  and name_key = 'readme.md'
  and ancestor_path >= '/'
  and ancestor_path < '0'
  and ancestor_path > ''
order by ancestor_path
limit 51;

explain (analyze, buffers)
select id, name, parent_id, ancestor_path
from node
where type = 'FILE'
  and name_key = 'readme.md'
  and ancestor_path >= :'subtree_from'
  and ancestor_path < :'subtree_to'
  and ancestor_path > ''
order by ancestor_path
limit 51;

explain (analyze, buffers)
select id, name, parent_id, ancestor_path
from node
where type = 'FILE'
  and name_key = 'readme.md'
  and ancestor_path >= '/'
  and ancestor_path < '0'
  and ancestor_path > '/1/99/'
order by ancestor_path
limit 51;

explain (analyze, buffers)
select id, parent_id, type, name, created_at
from node
where parent_id = :project_id
  and type = 'FILE'
  and name > ''
order by name
limit 51;

set plan_cache_mode = force_generic_plan;

prepare suggestions(text, text, int) as
select id, name, parent_id, ancestor_path
from node
where type = 'FILE'
  and name_key >= $1
  and name_key < $2
order by name_key, ancestor_path
limit $3;

explain (analyze, buffers)
execute suggestions('rep', 'rep' || chr(1114111), 10);

prepare files_by_name(text, text, text, text, int) as
select id, name, parent_id, ancestor_path
from node
where type = 'FILE'
  and name_key = $1
  and ancestor_path >= $2
  and ancestor_path < $3
  and ancestor_path > $4
order by ancestor_path
limit $5;

explain (analyze, buffers)
execute files_by_name('readme.md', :'subtree_from', :'subtree_to', '', 51);
