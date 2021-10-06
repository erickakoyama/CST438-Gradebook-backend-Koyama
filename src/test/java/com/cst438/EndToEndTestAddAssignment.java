package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseDTOG;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;

/*
 * This example shows how to use selenium testing using the web driver 
 * with Chrome browser.
 * 
 *  - Buttons, input, and anchor elements are located using XPATH expression.
 *  - onClick( ) method is used with buttons and anchor tags.
 *  - Input fields are located and sendKeys( ) method is used to enter test data.
 *  - Spring Boot JPA is used to initialize, verify and reset the database before
 *      and after testing.
 */

@SpringBootTest
public class EndToEndTestAddAssignment {

	public static final String CHROME_DRIVER_FILE_LOCATION = "/Users/erickakoyama/Desktop/chromedriver";

	public static final String URL = "https://cst438grade-fe-koyama.herokuapp.com";
	public static final String TEST_USER_EMAIL = "test@csumb.edu";
	public static final String TEST_INSTRUCTOR_EMAIL = "dwisneski@csumb.edu";
	public static final Integer TEST_COURSE_ID = 123456;
	public static final int SLEEP_DURATION = 1000; // 1 second.

	@Autowired
	EnrollmentRepository enrollmentRepository;

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	AssignmentGradeRepository assignnmentGradeRepository;

	@Autowired
	AssignmentRepository assignmentRepository;

	@Test
	public void addCourseTest() throws Exception {
		// container for test assignment
		Assignment a = null;

		// set the driver location and start driver
		//@formatter:off
		// browser	property name 				Java Driver Class
		// edge 	webdriver.edge.driver 		EdgeDriver
		// FireFox 	webdriver.firefox.driver 	FirefoxDriver
		// IE 		webdriver.ie.driver 		InternetExplorerDriver
		//@formatter:on

		System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
		WebDriver driver = new ChromeDriver();
		// Puts an Implicit wait for 10 seconds before throwing exception
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		driver.get(URL);
		Thread.sleep(SLEEP_DURATION);

		try {			
			// locate add assignment button
			driver.findElement(By.xpath("//button[span='Add Assignment']")).click();
			Thread.sleep(SLEEP_DURATION);
			
			// Locate input for assignment name
			driver.findElement(By.xpath("//input[@name='name']")).sendKeys("TEST");
			driver.findElement(By.xpath("//input[@name='dueDate']")).sendKeys("2021-09-05");
			driver.findElement(By.xpath("//input[@name='courseId']")).sendKeys("123456");
			Thread.sleep(SLEEP_DURATION);
			
			// locate submission button to add assignment
			driver.findElement(By.xpath("//button[span='Add']")).click();
			Thread.sleep(SLEEP_DURATION);
			
			// verify that assignment shows up
			WebElement we = driver.findElement(By.xpath("//div[@data-field='assignmentName' and @data-value='TEST']"));
			we =  we.findElement(By.xpath("following-sibling::div[@data-field='dueDate']"));
			assertEquals("2021-09-05", we.getAttribute("data-value"));
			
			// Fetch assignment to clean up after test is done
			List<Assignment> assignments = assignmentRepository.findNeedGradingByEmail(TEST_INSTRUCTOR_EMAIL);
			 
			for (Assignment assignment : assignments) {
				Boolean courseIdMatch = assignment.getCourse().getCourse_id() == TEST_COURSE_ID;
				Boolean assignmentNameMatch = assignment.getName().equals("TEST");
				String assignmentDueDate = new SimpleDateFormat("yyyy-MM-dd").format(assignment.getDueDate());
				Boolean dueDateMatch = assignmentDueDate.equals("2021-09-05");
				if (courseIdMatch && assignmentNameMatch && dueDateMatch) {
					 a = assignment;
				}
			 }
			 
			assertNotNull(a, "The assignment was not included in the list of assignments needing grading.");
		} catch (Exception ex) {
			throw ex;
		} finally {
			// clean up database.
			if (a != null) {
				assignmentRepository.delete(a);
			}
			
			driver.quit();
		}

	}
}
