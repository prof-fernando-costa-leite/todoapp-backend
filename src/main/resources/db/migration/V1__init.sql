create table app_users (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    created_at timestamp with time zone not null
);

create table app_groups (
    id uuid primary key,
    name varchar(120) not null,
    created_by uuid not null references app_users(id),
    created_at timestamp with time zone not null
);

create table group_members (
    group_id uuid not null references app_groups(id) on delete cascade,
    user_id uuid not null references app_users(id),
    member_role varchar(20) not null,
    added_at timestamp with time zone not null,
    primary key (group_id, user_id),
    constraint chk_group_members_role check (member_role in ('OWNER', 'MEMBER'))
);

create table boards (
    id uuid primary key,
    group_id uuid not null references app_groups(id) on delete cascade,
    name varchar(120) not null,
    description varchar(500),
    created_by uuid not null references app_users(id),
    created_at timestamp with time zone not null
);

create table board_statuses (
    id uuid primary key,
    board_id uuid not null references boards(id) on delete cascade,
    code varchar(40) not null,
    name varchar(80) not null,
    status_kind varchar(20) not null,
    rank integer not null,
    is_initial boolean not null default false,
    is_terminal boolean not null default false,
    created_at timestamp with time zone not null,
    constraint uq_board_status_code unique (board_id, code),
    constraint chk_board_status_kind check (status_kind in ('SYSTEM', 'CUSTOM'))
);

create table board_status_transitions (
    board_id uuid not null references boards(id) on delete cascade,
    from_status_id uuid not null references board_statuses(id) on delete cascade,
    to_status_id uuid not null references board_statuses(id) on delete cascade,
    primary key (board_id, from_status_id, to_status_id)
);

create table tasks (
    id uuid primary key,
    board_id uuid not null references boards(id) on delete cascade,
    creator_id uuid not null references app_users(id),
    assignee_id uuid references app_users(id),
    title varchar(200) not null,
    description varchar(2000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    status_id uuid not null references board_statuses(id),
    points integer,
    priority varchar(20) not null,
    blocking_task_id uuid references tasks(id),
    constraint chk_tasks_priority check (priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    constraint chk_tasks_points_non_negative check (points is null or points >= 0)
);

create table task_status_history (
    id uuid primary key,
    task_id uuid not null references tasks(id) on delete cascade,
    from_status_id uuid references board_statuses(id),
    to_status_id uuid not null references board_statuses(id),
    changed_by uuid not null references app_users(id),
    changed_at timestamp with time zone not null
);

create index idx_group_members_user_id on group_members(user_id);
create index idx_boards_group_id on boards(group_id);
create index idx_board_statuses_board_id on board_statuses(board_id);
create index idx_board_status_transitions_board_id on board_status_transitions(board_id);
create index idx_tasks_board_id on tasks(board_id);
create index idx_tasks_creator_id on tasks(creator_id);
create index idx_tasks_assignee_id on tasks(assignee_id);
create index idx_task_status_history_task_id on task_status_history(task_id);

