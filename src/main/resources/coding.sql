CREATE TABLE coding_topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    parent_id BIGINT,
    display_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_coding_topic_parent
        FOREIGN KEY (parent_id)
        REFERENCES coding_topics(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_coding_topic_name_parent
        UNIQUE (name, parent_id)
);

CREATE TABLE coding_questions (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    companies TEXT[] NOT NULL DEFAULT '{}',
    platform VARCHAR(20) NOT NULL,
    problem_url TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_coding_question_topic
        FOREIGN KEY (topic_id)
        REFERENCES coding_topics(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_coding_question_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),

    CONSTRAINT chk_coding_question_platform
        CHECK (platform IN ('LEETCODE', 'GFG')),

    CONSTRAINT uq_coding_question_url
        UNIQUE (problem_url)
);

CREATE TABLE user_question_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    solved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_progress_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_progress_question
        FOREIGN KEY (question_id)
        REFERENCES coding_questions(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_question_progress
        UNIQUE (user_id, question_id)
);

CREATE INDEX idx_coding_topics_parent
ON coding_topics(parent_id);

CREATE INDEX idx_coding_questions_topic
ON coding_questions(topic_id);

CREATE INDEX idx_user_progress_user
ON user_question_progress(user_id);


DELETE FROM coding_topics;
DELETE FROM coding_questions;


INSERT INTO coding_topics (id, name, parent_id, display_order)
VALUES
(1,  'Arrays', NULL, 1),
(2,  'Strings', NULL, 2),
(3,  'Binary Search', NULL, 3),
(4,  'Linked List', NULL, 4),
(5,  'Stack & Queue', NULL, 5),
(6,  'Trees', NULL, 6),
(7,  'Heap / Priority Queue', NULL, 7),
(8,  'Graphs', NULL, 8),
(9,  'Dynamic Programming', NULL, 9),
(10, 'Trie', NULL, 10),
(11, 'Sliding Window & Two Pointers', NULL, 11),

(12, 'Recursion', NULL, 12),
(13, 'Backtracking', NULL, 13),
(14, 'Bit Manipulation', NULL, 14),
(15, 'Hashing', NULL, 15),
(16, 'Binary Search Tree', NULL, 16),
(17, 'Greedy', NULL, 17),
(18, 'Intervals', NULL, 18),
(19, 'Math', NULL, 19),
(20, 'Sorting', NULL, 20),

(21, 'Prefix Sum', NULL, 21),
(22, 'Matrix', NULL, 22),
(23, 'Disjoint Set Union', NULL, 23),
(24, 'Advanced Graphs', NULL, 24),
(25, 'Advanced Data Structures', NULL, 25)

ON CONFLICT DO NOTHING;


INSERT INTO coding_topics (id, name, parent_id, display_order)
VALUES

-- =====================================================
-- ARRAYS
-- =====================================================
(101, 'Array Basics', 1, 1),
(102, 'Easy Array Problems', 1, 2),
(103, 'Medium Array Problems', 1, 3),
(104, 'Hard Array Problems', 1, 4),
(105, 'Subarrays', 1, 5),
(106, 'Kadane Algorithm', 1, 6),
(107, 'Array Rearrangement', 1, 7),
(108, 'Majority Element', 1, 8),
(109, 'Missing and Repeating Numbers', 1, 9),
(110, 'Inversion Problems', 1, 10),

-- =====================================================
-- STRINGS
-- =====================================================
(201, 'String Basics', 2, 1),
(202, 'Palindrome Problems', 2, 2),
(203, 'Anagram Problems', 2, 3),
(204, 'String Frequency Problems', 2, 4),
(205, 'String Transformation', 2, 5),
(206, 'String Matching', 2, 6),
(207, 'KMP Algorithm', 2, 7),
(208, 'Rabin Karp', 2, 8),
(209, 'Z Algorithm', 2, 9),
(210, 'Advanced String Problems', 2, 10),

-- =====================================================
-- BINARY SEARCH
-- =====================================================
(301, 'Binary Search Basics', 3, 1),
(302, 'Lower Bound and Upper Bound', 3, 2),
(303, 'Search in Rotated Arrays', 3, 3),
(304, 'Binary Search on Answers', 3, 4),
(305, 'Binary Search on 2D Arrays', 3, 5),
(306, 'Peak Element Problems', 3, 6),
(307, 'Median Problems', 3, 7),
(308, 'Advanced Binary Search', 3, 8),

-- =====================================================
-- LINKED LIST
-- =====================================================
(401, 'Linked List Basics', 4, 1),
(402, 'Linked List Insertion and Deletion', 4, 2),
(403, 'Linked List Reversal', 4, 3),
(404, 'Fast and Slow Pointer', 4, 4),
(405, 'Linked List Cycle', 4, 5),
(406, 'Merge Linked Lists', 4, 6),
(407, 'Doubly Linked List', 4, 7),
(408, 'Linked List Sorting', 4, 8),
(409, 'Advanced Linked List Problems', 4, 9),

-- =====================================================
-- STACK & QUEUE
-- =====================================================
(501, 'Stack Basics', 5, 1),
(502, 'Queue Basics', 5, 2),
(503, 'Stack Implementation', 5, 3),
(504, 'Queue Implementation', 5, 4),
(505, 'Monotonic Stack', 5, 5),
(506, 'Monotonic Queue', 5, 6),
(507, 'Next Greater Element', 5, 7),
(508, 'Parentheses Problems', 5, 8),
(509, 'Expression Problems', 5, 9),
(510, 'Advanced Stack and Queue', 5, 10),

-- =====================================================
-- TREES
-- =====================================================
(601, 'Tree Basics', 6, 1),
(602, 'Tree Traversals', 6, 2),
(603, 'Level Order Traversal', 6, 3),
(604, 'Tree Height and Depth', 6, 4),
(605, 'Tree Diameter', 6, 5),
(606, 'Balanced Binary Tree', 6, 6),
(607, 'Tree Views', 6, 7),
(608, 'Boundary Traversal', 6, 8),
(609, 'Lowest Common Ancestor', 6, 9),
(610, 'Root to Node Paths', 6, 10),
(611, 'Tree Construction', 6, 11),
(612, 'Serialize and Deserialize', 6, 12),
(613, 'Advanced Binary Tree Problems', 6, 13),

-- =====================================================
-- HEAP / PRIORITY QUEUE
-- =====================================================
(701, 'Heap Basics', 7, 1),
(702, 'Min Heap and Max Heap', 7, 2),
(703, 'Kth Element Problems', 7, 3),
(704, 'Top K Problems', 7, 4),
(705, 'Merge K Problems', 7, 5),
(706, 'Heap Sorting Problems', 7, 6),
(707, 'Median Using Heap', 7, 7),
(708, 'Advanced Heap Problems', 7, 8),

-- =====================================================
-- GRAPHS
-- =====================================================
(801, 'Graph Basics', 8, 1),
(802, 'BFS', 8, 2),
(803, 'DFS', 8, 3),
(804, 'Connected Components', 8, 4),
(805, 'Flood Fill Problems', 8, 5),
(806, 'Cycle Detection', 8, 6),
(807, 'Bipartite Graph', 8, 7),
(808, 'Topological Sort', 8, 8),
(809, 'Directed Graph Problems', 8, 9),
(810, 'Undirected Graph Problems', 8, 10),
(811, 'Shortest Path', 8, 11),
(812, 'Dijkstra Algorithm', 8, 12),
(813, 'Bellman Ford Algorithm', 8, 13),
(814, 'Floyd Warshall Algorithm', 8, 14),
(815, 'Minimum Spanning Tree', 8, 15),
(816, 'Strongly Connected Components', 8, 16),
(817, 'Bridges and Articulation Points', 8, 17),

-- =====================================================
-- DYNAMIC PROGRAMMING
-- =====================================================
(901, 'DP Introduction', 9, 1),
(902, '1D Dynamic Programming', 9, 2),
(903, '2D Dynamic Programming', 9, 3),
(904, 'Grid DP', 9, 4),
(905, 'Subsequence DP', 9, 5),
(906, 'Subset DP', 9, 6),
(907, '0/1 Knapsack', 9, 7),
(908, 'Unbounded Knapsack', 9, 8),
(909, 'Coin Change', 9, 9),
(910, 'Longest Increasing Subsequence', 9, 10),
(911, 'Longest Common Subsequence', 9, 11),
(912, 'String Dynamic Programming', 9, 12),
(913, 'Palindrome DP', 9, 13),
(914, 'Stock DP', 9, 14),
(915, 'Partition DP', 9, 15),
(916, 'Matrix Chain Multiplication', 9, 16),
(917, 'DP on Trees', 9, 17),
(918, 'DP on Graphs', 9, 18),
(919, 'Advanced Dynamic Programming', 9, 19),

-- =====================================================
-- TRIE
-- =====================================================
(1001, 'Trie Basics', 10, 1),
(1002, 'Trie Insert Search Delete', 10, 2),
(1003, 'Prefix Problems', 10, 3),
(1004, 'Word Dictionary Problems', 10, 4),
(1005, 'XOR Trie', 10, 5),
(1006, 'Advanced Trie Problems', 10, 6),

-- =====================================================
-- SLIDING WINDOW & TWO POINTERS
-- =====================================================
(1101, 'Two Pointer Basics', 11, 1),
(1102, 'Opposite Direction Two Pointers', 11, 2),
(1103, 'Same Direction Two Pointers', 11, 3),
(1104, 'Fixed Size Sliding Window', 11, 4),
(1105, 'Variable Size Sliding Window', 11, 5),
(1106, 'Substring Window Problems', 11, 6),
(1107, 'Subarray Window Problems', 11, 7),
(1108, 'Advanced Sliding Window', 11, 8),

-- =====================================================
-- RECURSION
-- =====================================================
(1201, 'Recursion Basics', 12, 1),
(1202, 'Recursion on Arrays', 12, 2),
(1203, 'Recursion on Strings', 12, 3),
(1204, 'Subsequence Generation', 12, 4),
(1205, 'Recursive Sorting', 12, 5),
(1206, 'Advanced Recursion', 12, 6),

-- =====================================================
-- BACKTRACKING
-- =====================================================
(1301, 'Backtracking Basics', 13, 1),
(1302, 'Subsets', 13, 2),
(1303, 'Combinations', 13, 3),
(1304, 'Permutations', 13, 4),
(1305, 'Combination Sum', 13, 5),
(1306, 'N Queens', 13, 6),
(1307, 'Sudoku', 13, 7),
(1308, 'Maze Problems', 13, 8),
(1309, 'Word Search', 13, 9),
(1310, 'Advanced Backtracking', 13, 10),

-- =====================================================
-- BIT MANIPULATION
-- =====================================================
(1401, 'Bit Manipulation Basics', 14, 1),
(1402, 'Bit Operations', 14, 2),
(1403, 'Set and Unset Bits', 14, 3),
(1404, 'Counting Bits', 14, 4),
(1405, 'XOR Problems', 14, 5),
(1406, 'Bitmasking', 14, 6),
(1407, 'Power Set Using Bits', 14, 7),
(1408, 'Advanced Bit Manipulation', 14, 8),

-- =====================================================
-- HASHING
-- =====================================================
(1501, 'Hashing Basics', 15, 1),
(1502, 'Frequency Counting', 15, 2),
(1503, 'HashMap Problems', 15, 3),
(1504, 'HashSet Problems', 15, 4),
(1505, 'Prefix Sum with Hashing', 15, 5),
(1506, 'Subarray Hashing Problems', 15, 6),
(1507, 'Advanced Hashing', 15, 7),

-- =====================================================
-- BINARY SEARCH TREE
-- =====================================================
(1601, 'BST Basics', 16, 1),
(1602, 'Search in BST', 16, 2),
(1603, 'Insert and Delete in BST', 16, 3),
(1604, 'Floor and Ceil in BST', 16, 4),
(1605, 'Kth Element in BST', 16, 5),
(1606, 'Validate BST', 16, 6),
(1607, 'Lowest Common Ancestor in BST', 16, 7),
(1608, 'BST Iterator', 16, 8),
(1609, 'Construct BST', 16, 9),
(1610, 'Advanced BST Problems', 16, 10),

-- =====================================================
-- GREEDY
-- =====================================================
(1701, 'Greedy Basics', 17, 1),
(1702, 'Scheduling Problems', 17, 2),
(1703, 'Activity Selection', 17, 3),
(1704, 'Job Sequencing', 17, 4),
(1705, 'Meeting Problems', 17, 5),
(1706, 'Jump Problems', 17, 6),
(1707, 'Candy and Distribution Problems', 17, 7),
(1708, 'Advanced Greedy', 17, 8),

-- =====================================================
-- INTERVALS
-- =====================================================
(1801, 'Interval Basics', 18, 1),
(1802, 'Merge Intervals', 18, 2),
(1803, 'Insert Interval', 18, 3),
(1804, 'Overlapping Intervals', 18, 4),
(1805, 'Meeting Intervals', 18, 5),
(1806, 'Advanced Interval Problems', 18, 6),

-- =====================================================
-- MATH
-- =====================================================
(1901, 'Math Basics', 19, 1),
(1902, 'GCD and LCM', 19, 2),
(1903, 'Prime Numbers', 19, 3),
(1904, 'Sieve of Eratosthenes', 19, 4),
(1905, 'Modular Arithmetic', 19, 5),
(1906, 'Fast Exponentiation', 19, 6),
(1907, 'Number Theory', 19, 7),
(1908, 'Combinatorics', 19, 8),

-- =====================================================
-- SORTING
-- =====================================================
(2001, 'Sorting Basics', 20, 1),
(2002, 'Bubble Sort', 20, 2),
(2003, 'Selection Sort', 20, 3),
(2004, 'Insertion Sort', 20, 4),
(2005, 'Merge Sort', 20, 5),
(2006, 'Quick Sort', 20, 6),
(2007, 'Counting Sort', 20, 7),
(2008, 'Sorting Interview Problems', 20, 8),

-- =====================================================
-- PREFIX SUM
-- =====================================================
(2101, 'Prefix Sum Basics', 21, 1),
(2102, 'Prefix Sum Subarrays', 21, 2),
(2103, '2D Prefix Sum', 21, 3),
(2104, 'Difference Array', 21, 4),
(2105, 'Prefix Sum with HashMap', 21, 5),

-- =====================================================
-- MATRIX
-- =====================================================
(2201, 'Matrix Basics', 22, 1),
(2202, 'Matrix Traversal', 22, 2),
(2203, 'Matrix Rotation', 22, 3),
(2204, 'Spiral Matrix', 22, 4),
(2205, 'Matrix Search', 22, 5),
(2206, 'Grid Simulation Problems', 22, 6),

-- =====================================================
-- DISJOINT SET UNION
-- =====================================================
(2301, 'DSU Basics', 23, 1),
(2302, 'Union by Rank and Size', 23, 2),
(2303, 'Connected Components using DSU', 23, 3),
(2304, 'Cycle Detection using DSU', 23, 4),
(2305, 'DSU Interview Problems', 23, 5),

-- =====================================================
-- ADVANCED GRAPHS
-- =====================================================
(2401, 'Kosaraju Algorithm', 24, 1),
(2402, 'Tarjan Algorithm', 24, 2),
(2403, 'Eulerian Path and Circuit', 24, 3),
(2404, 'Network Flow', 24, 4),
(2405, 'Advanced Shortest Path', 24, 5),

-- =====================================================
-- ADVANCED DATA STRUCTURES
-- =====================================================
(2501, 'Segment Tree', 25, 1),
(2502, 'Lazy Propagation', 25, 2),
(2503, 'Fenwick Tree', 25, 3),
(2504, 'Ordered Set Problems', 25, 4),
(2505, 'Advanced Range Queries', 25, 5)

ON CONFLICT DO NOTHING;

select * from coding_questions;

