insert into node (parent_id, type, name, name_key, ancestor_path)
select root.id, 'FOLDER', 'team-' || lpad(t::text, 3, '0'), 'team-' || lpad(t::text, 3, '0'),
       root.ancestor_path || root.id || '/'
from node root, generate_series(1, 100) t
where root.parent_id is null;

insert into node (parent_id, type, name, name_key, ancestor_path)
select team.id, 'FOLDER', 'project-' || lpad(p::text, 3, '0'), 'project-' || lpad(p::text, 3, '0'),
       team.ancestor_path || team.id || '/'
from node team, generate_series(1, 100) p
where team.type = 'FOLDER' and team.name like 'team-%';

insert into node (parent_id, type, name, name_key, ancestor_path)
select project.id, 'FILE', file.name, lower(file.name), project.ancestor_path || project.id || '/'
from node project
cross join lateral (
    select case
               when f = 1 then 'README.md'
               else (array['report', 'notes', 'invoice', 'photo', 'draft'])[1 + f % 5]
                   || '-' || lpad((project.id * 100 + f)::text, 8, '0')
                   || (array['.pdf', '.txt', '.pdf', '.jpg', '.docx'])[1 + f % 5]
           end as name
    from generate_series(1, 100) f
) file
where project.type = 'FOLDER' and project.name like 'project-%';

analyze node;
