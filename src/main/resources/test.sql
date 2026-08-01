SELECT current_database(), current_schema();

SELECT COUNT(*) AS questions
FROM public.coding_questions;

SELECT COUNT(*) AS topics
FROM public.coding_topics;

SELECT COUNT(*) AS root_topics
FROM public.coding_topics
WHERE parent_id IS NULL;