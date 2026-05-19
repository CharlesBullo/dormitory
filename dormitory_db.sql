-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 19, 2026 at 01:19 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `dormitory_db`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `EvictTenant` (IN `p_tenant_id` VARCHAR(20))   BEGIN
    DECLARE v_room_id VARCHAR(10);
    
    -- Get room_id
    SELECT room_id INTO v_room_id 
    FROM tenants WHERE tenant_id = p_tenant_id;
    
    -- Update tenant as evicted
    UPDATE tenants 
    SET is_evicted = TRUE 
    WHERE tenant_id = p_tenant_id;
    
    -- Update room status to VACANT
    IF v_room_id IS NOT NULL THEN
        UPDATE rooms 
        SET status = 'VACANT', rent_due = 0, tenant_name = NULL, tenant_id = NULL
        WHERE room_id = v_room_id;
    END IF;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `RecordPayment` (IN `p_tenant_id` VARCHAR(20), IN `p_amount` DECIMAL(10,2), IN `p_months_paid` INT, IN `p_payment_method` VARCHAR(20))   BEGIN
    DECLARE v_monthly_rent DECIMAL(10,2);
    
    -- Get monthly rent
    SELECT monthly_rent INTO v_monthly_rent 
    FROM tenants WHERE tenant_id = p_tenant_id;
    
    -- Insert payment record
    INSERT INTO payments (
        tenant_id, amount, payment_date, months_paid, payment_method
    ) VALUES (
        p_tenant_id, p_amount, CURDATE(), p_months_paid, p_payment_method
    );
    
    -- Update tenant payment status
    UPDATE tenants 
    SET 
        is_paid = TRUE,
        total_debt = 0,
        missed_months = 0,
        paid_streak = paid_streak + 1,
        is_evicted = FALSE,
        due_date = DATE_ADD(due_date, INTERVAL p_months_paid MONTH)
    WHERE tenant_id = p_tenant_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `RegisterTenant` (IN `p_tenant_id` VARCHAR(20), IN `p_tenant_name` VARCHAR(100), IN `p_contact_number` VARCHAR(20), IN `p_email` VARCHAR(100), IN `p_address` TEXT, IN `p_room_id` VARCHAR(10), IN `p_move_in_date` DATE)   BEGIN
    DECLARE v_monthly_rent DECIMAL(10,2);
    DECLARE v_room_number VARCHAR(10);
    DECLARE v_due_date DATE;
    
    -- Get room monthly rent and number
    SELECT monthly_rent, room_number INTO v_monthly_rent, v_room_number
    FROM rooms WHERE room_id = p_room_id;
    
    -- Calculate due date (1 month after move-in)
    SET v_due_date = DATE_ADD(p_move_in_date, INTERVAL 1 MONTH);
    
    -- Insert tenant
    INSERT INTO tenants (
        tenant_id, tenant_name, contact_number, email, address,
        room_id, room_number, monthly_rent, move_in_date, due_date
    ) VALUES (
        p_tenant_id, p_tenant_name, p_contact_number, p_email, p_address,
        p_room_id, v_room_number, v_monthly_rent, p_move_in_date, v_due_date
    );
    
    -- Update room status to OCCUPIED
    UPDATE rooms SET status = 'OCCUPIED', tenant_name = p_tenant_name, tenant_id = p_tenant_id
    WHERE room_id = p_room_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `RemoveTenant` (IN `p_tenant_id` VARCHAR(20))   BEGIN
    DECLARE v_room_id VARCHAR(10);
    
    -- Get room_id
    SELECT room_id INTO v_room_id 
    FROM tenants WHERE tenant_id = p_tenant_id;
    
    -- Delete tenant (cascades to payments)
    DELETE FROM tenants WHERE tenant_id = p_tenant_id;
    
    -- Update room status to VACANT
    IF v_room_id IS NOT NULL THEN
        UPDATE rooms 
        SET status = 'VACANT', rent_due = 0, tenant_name = NULL, tenant_id = NULL
        WHERE room_id = v_room_id;
    END IF;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `activity_logs`
--

CREATE TABLE `activity_logs` (
  `log_id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `action` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Stand-in structure for view `available_rooms`
-- (See below for the actual view)
--
CREATE TABLE `available_rooms` (
`room_id` varchar(10)
,`room_number` varchar(10)
,`monthly_rent` decimal(10,2)
,`status` enum('VACANT','OCCUPIED','MAINTENANCE')
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `monthly_revenue`
-- (See below for the actual view)
--
CREATE TABLE `monthly_revenue` (
`month` varchar(7)
,`payment_count` bigint(21)
,`total_amount` decimal(32,2)
);

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `payment_id` int(11) NOT NULL,
  `tenant_id` varchar(20) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `payment_date` date NOT NULL,
  `months_paid` int(11) NOT NULL,
  `payment_method` enum('CASH','BANK_TRANSFER','CHECK','ONLINE') DEFAULT 'CASH',
  `reference_number` varchar(50) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `payments`
--

INSERT INTO `payments` (`payment_id`, `tenant_id`, `amount`, `payment_date`, `months_paid`, `payment_method`, `reference_number`, `notes`, `created_at`) VALUES
(1, 'T1779104796383', 5000.00, '2026-05-18', 1, 'CASH', 'PAY-1779104909153', NULL, '2026-05-18 11:48:29'),
(2, 'T1779104796383', 5000.00, '2026-05-18', 1, 'CASH', 'PAY-1779106743513', NULL, '2026-05-18 12:19:03');

-- --------------------------------------------------------

--
-- Table structure for table `rooms`
--

CREATE TABLE `rooms` (
  `room_id` varchar(10) NOT NULL,
  `room_number` varchar(10) NOT NULL,
  `status` enum('VACANT','OCCUPIED','MAINTENANCE') DEFAULT 'VACANT',
  `monthly_rent` decimal(10,2) NOT NULL,
  `rent_due` decimal(10,2) DEFAULT 0.00,
  `tenant_name` varchar(100) DEFAULT NULL,
  `tenant_id` varchar(20) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `rooms`
--

INSERT INTO `rooms` (`room_id`, `room_number`, `status`, `monthly_rent`, `rent_due`, `tenant_name`, `tenant_id`, `created_at`, `updated_at`) VALUES
('R001', '101', 'OCCUPIED', 5000.00, 0.00, 'dave bullo', 'T1779104796383', '2026-05-12 12:37:20', '2026-05-18 12:24:22'),
('R002', '102', 'VACANT', 4500.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-19 11:18:51'),
('R003', '103', 'VACANT', 5500.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20'),
('R004', '104', 'VACANT', 5000.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-18 12:23:05'),
('R005', '105', 'VACANT', 4800.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20'),
('R006', '201', 'VACANT', 6000.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20'),
('R007', '202', 'VACANT', 5200.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20'),
('R008', '203', 'VACANT', 5500.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20'),
('R009', '204', 'VACANT', 5000.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20'),
('R010', '205', 'VACANT', 5800.00, 0.00, NULL, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20');

-- --------------------------------------------------------

--
-- Table structure for table `tenants`
--

CREATE TABLE `tenants` (
  `tenant_id` varchar(20) NOT NULL,
  `tenant_name` varchar(100) NOT NULL,
  `contact_number` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `address` text DEFAULT NULL,
  `room_id` varchar(10) DEFAULT NULL,
  `room_number` varchar(10) DEFAULT NULL,
  `monthly_rent` decimal(10,2) NOT NULL,
  `is_paid` tinyint(1) DEFAULT 0,
  `total_debt` decimal(10,2) DEFAULT 0.00,
  `missed_months` int(11) DEFAULT 0,
  `is_evicted` tinyint(1) DEFAULT 0,
  `paid_streak` int(11) DEFAULT 0,
  `move_in_date` date NOT NULL,
  `due_date` date NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tenants`
--

INSERT INTO `tenants` (`tenant_id`, `tenant_name`, `contact_number`, `email`, `address`, `room_id`, `room_number`, `monthly_rent`, `is_paid`, `total_debt`, `missed_months`, `is_evicted`, `paid_streak`, `move_in_date`, `due_date`, `created_at`, `updated_at`) VALUES
('T1779104796383', 'dave bullo', '999', 'dave@gmail.com', 'yes', 'R001', '101', 5000.00, 1, 0.00, 0, 0, 2, '2026-05-18', '2026-08-18', '2026-05-18 11:46:36', '2026-05-18 12:19:03'),
('T1779107105636', 'charles', '666', 'charles@gmail.com', 'yes', 'R002', '102', 4500.00, 0, 0.00, 0, 1, 0, '2026-05-18', '2026-06-18', '2026-05-18 12:25:05', '2026-05-18 12:26:27');

--
-- Triggers `tenants`
--
DELIMITER $$
CREATE TRIGGER `update_room_on_tenant_change` AFTER UPDATE ON `tenants` FOR EACH ROW BEGIN
    IF NEW.room_id IS NOT NULL AND NEW.is_evicted = FALSE THEN
        UPDATE rooms 
        SET tenant_name = NEW.tenant_name,
            tenant_id = NEW.tenant_id
        WHERE room_id = NEW.room_id;
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `tenant_payment_summary`
-- (See below for the actual view)
--
CREATE TABLE `tenant_payment_summary` (
`tenant_id` varchar(20)
,`tenant_name` varchar(100)
,`room_number` varchar(10)
,`monthly_rent` decimal(10,2)
,`total_debt` decimal(10,2)
,`missed_months` int(11)
,`due_date` date
,`total_paid` decimal(32,2)
,`payment_count` bigint(21)
);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `role` enum('ADMIN','STAFF','TENANT') DEFAULT 'STAFF',
  `is_active` tinyint(1) DEFAULT 1,
  `last_login` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `tenant_id` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password`, `full_name`, `email`, `role`, `is_active`, `last_login`, `created_at`, `updated_at`, `tenant_id`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrJwqRqDxrpoFZqq2QKKhYrqXcgJq1K', 'Administrator', 'admin@dormitory.com', 'ADMIN', 1, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20', NULL),
(2, 'staff1', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrJwqRqDxrpoFZqq2QKKhYrqXcgJq1K', 'John Staff', 'staff@dormitory.com', 'STAFF', 1, NULL, '2026-05-12 12:37:20', '2026-05-12 12:37:20', NULL),
(5, 'dave', '$2a$10$dhANYsafpskONJ.jTl5deuXcgKqxmkr/CgMkuc8ss87H8Nv1doZ56', 'dave bullo', 'dave@gmail.com', 'TENANT', 1, '2026-05-18 12:35:29', '2026-05-18 11:46:36', '2026-05-18 12:35:29', 'T1779104796383'),
(13, 'charles', '$2a$10$zGDEPMPs9XzYSDWQK0BLU.ldQphtUW/X4RY6Q91xyvZkp8OyBruEG', 'charles', 'charles@gmail.com', 'TENANT', 1, '2026-05-18 12:35:13', '2026-05-18 12:25:05', '2026-05-18 12:35:13', 'T1779107105636');

-- --------------------------------------------------------

--
-- Structure for view `available_rooms`
--
DROP TABLE IF EXISTS `available_rooms`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `available_rooms`  AS SELECT `rooms`.`room_id` AS `room_id`, `rooms`.`room_number` AS `room_number`, `rooms`.`monthly_rent` AS `monthly_rent`, `rooms`.`status` AS `status` FROM `rooms` WHERE `rooms`.`status` = 'VACANT' ORDER BY `rooms`.`room_number` ASC ;

-- --------------------------------------------------------

--
-- Structure for view `monthly_revenue`
--
DROP TABLE IF EXISTS `monthly_revenue`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `monthly_revenue`  AS SELECT date_format(`payments`.`payment_date`,'%Y-%m') AS `month`, count(0) AS `payment_count`, sum(`payments`.`amount`) AS `total_amount` FROM `payments` GROUP BY date_format(`payments`.`payment_date`,'%Y-%m') ORDER BY date_format(`payments`.`payment_date`,'%Y-%m') DESC ;

-- --------------------------------------------------------

--
-- Structure for view `tenant_payment_summary`
--
DROP TABLE IF EXISTS `tenant_payment_summary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tenant_payment_summary`  AS SELECT `t`.`tenant_id` AS `tenant_id`, `t`.`tenant_name` AS `tenant_name`, `t`.`room_number` AS `room_number`, `t`.`monthly_rent` AS `monthly_rent`, `t`.`total_debt` AS `total_debt`, `t`.`missed_months` AS `missed_months`, `t`.`due_date` AS `due_date`, coalesce(sum(`p`.`amount`),0) AS `total_paid`, count(`p`.`payment_id`) AS `payment_count` FROM (`tenants` `t` left join `payments` `p` on(`t`.`tenant_id` = `p`.`tenant_id`)) WHERE `t`.`is_evicted` = 0 GROUP BY `t`.`tenant_id` ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activity_logs`
--
ALTER TABLE `activity_logs`
  ADD PRIMARY KEY (`log_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_created_at` (`created_at`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`payment_id`),
  ADD KEY `idx_tenant_id` (`tenant_id`),
  ADD KEY `idx_payment_date` (`payment_date`);

--
-- Indexes for table `rooms`
--
ALTER TABLE `rooms`
  ADD PRIMARY KEY (`room_id`),
  ADD UNIQUE KEY `room_number` (`room_number`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_room_number` (`room_number`);

--
-- Indexes for table `tenants`
--
ALTER TABLE `tenants`
  ADD PRIMARY KEY (`tenant_id`),
  ADD KEY `idx_tenant_name` (`tenant_name`),
  ADD KEY `idx_room_id` (`room_id`),
  ADD KEY `idx_is_evicted` (`is_evicted`),
  ADD KEY `idx_due_date` (`due_date`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `idx_username` (`username`),
  ADD KEY `idx_role` (`role`),
  ADD KEY `idx_users_tenant_id` (`tenant_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activity_logs`
--
ALTER TABLE `activity_logs`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `payment_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activity_logs`
--
ALTER TABLE `activity_logs`
  ADD CONSTRAINT `activity_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `payments_ibfk_1` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`tenant_id`) ON DELETE CASCADE;

--
-- Constraints for table `tenants`
--
ALTER TABLE `tenants`
  ADD CONSTRAINT `tenants_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL;

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `users_ibfk_1` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`tenant_id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
