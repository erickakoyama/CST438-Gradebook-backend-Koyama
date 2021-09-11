package com.cst438;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.cst438.controllers.GradeBookController;
import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentListDTO;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.GradebookDTO;
import com.cst438.domain.NewAssignmentFields;
import com.cst438.services.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.test.context.ContextConfiguration;

/* 
 * Example of using Junit with Mockito for mock objects
 *  the database repositories are mocked with test data.
 *  
 * Mockmvc is used to test a simulated REST call to the RestController
 * 
 * the http response and repository is verified.
 * 
 *   Note: This tests uses Junit 5.
 *  ContextConfiguration identifies the controller class to be tested
 *  addFilters=false turns off security.  (I could not get security to work in test environment.)
 *  WebMvcTest is needed for test environment to create Repository classes.
 */
@ContextConfiguration(classes = { GradeBookController.class })
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest
public class JunitTestGradebook {

	static final String URL = "http://localhost:8080";
	public static final int TEST_COURSE_ID = 40442;
	public static final String TEST_STUDENT_EMAIL = "test@csumb.edu";
	public static final String TEST_STUDENT_NAME = "test";
	public static final String TEST_INSTRUCTOR_EMAIL = "dwisneski@csumb.edu";
	public static final int TEST_YEAR = 2021;
	public static final String TEST_SEMESTER = "Fall";
	public static final String TEST_ASSIGNMENT_NAME = "Assignment 1";

	@MockBean
	AssignmentRepository assignmentRepository;

	@MockBean
	AssignmentGradeRepository assignmentGradeRepository;

	@MockBean
	CourseRepository courseRepository; // must have this to keep Spring test happy

	@MockBean
	RegistrationService registrationService; // must have this to keep Spring test happy

	@Autowired
	private MockMvc mvc;

	@Test
	public void gradeAssignment() throws Exception {

		MockHttpServletResponse response;

		// mock database data

		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());

		Enrollment enrollment = new Enrollment();
		enrollment.setCourse(course);
		course.getEnrollments().add(enrollment);
		enrollment.setId(TEST_COURSE_ID);
		enrollment.setStudentEmail(TEST_STUDENT_EMAIL);
		enrollment.setStudentName(TEST_STUDENT_NAME);

		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		course.getAssignments().add(assignment);
		// set dueDate to 1 week before now.
		assignment.setDueDate(new java.sql.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
		assignment.setId(1);
		assignment.setName("Assignment 1");
		assignment.setNeedsGrading(1);
		Optional<Assignment> optA = Optional.of(assignment);

		AssignmentGrade ag = new AssignmentGrade();
		ag.setAssignment(assignment);
		ag.setId(1);
		ag.setScore("");
		ag.setStudentEnrollment(enrollment);

		// given -- stubs for database repositories that return test data
		given(assignmentRepository.findById(1)).willReturn(optA);
		given(assignmentGradeRepository.findByAssignmentIdAndStudentEmail(1, TEST_STUDENT_EMAIL)).willReturn(null);
		given(assignmentGradeRepository.save(any())).willReturn(ag);

		// end of mock data

		// then do an http get request for assignment 1
		response = mvc.perform(MockMvcRequestBuilders.get("/gradebook/1").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify return data with entry for one student without no score
		assertEquals(200, response.getStatus());

		// verify that a save was called on repository
		verify(assignmentGradeRepository, times(1)).save(any()); // ???

		// verify that returned data has non zero primary key
		GradebookDTO result = fromJsonString(response.getContentAsString(), GradebookDTO.class);
		// assignment id is 1
		assertEquals(1, result.assignmentId);
		// there is one student list
		assertEquals(1, result.grades.size());
		assertEquals(TEST_STUDENT_NAME, result.grades.get(0).name);
		assertEquals("", result.grades.get(0).grade);

		// change grade to score = 80
		result.grades.get(0).grade = "80";

		given(assignmentGradeRepository.findById(1)).willReturn(ag);

		// send updates to server
		response = mvc
				.perform(MockMvcRequestBuilders.put("/gradebook/1").accept(MediaType.APPLICATION_JSON)
						.content(asJsonString(result)).contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());

		AssignmentGrade updatedag = new AssignmentGrade();
		updatedag.setId(1);
		updatedag.setScore("80");

		// verify that repository saveAll method was called
		verify(assignmentGradeRepository, times(1)).save(updatedag);
	}

	@Test
	public void updateAssignmentGrade() throws Exception {

		MockHttpServletResponse response;

		// mock database data

		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());

		Enrollment enrollment = new Enrollment();
		enrollment.setCourse(course);
		course.getEnrollments().add(enrollment);
		enrollment.setId(TEST_COURSE_ID);
		enrollment.setStudentEmail(TEST_STUDENT_EMAIL);
		enrollment.setStudentName(TEST_STUDENT_NAME);

		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		course.getAssignments().add(assignment);
		// set dueDate to 1 week before now.
		assignment.setDueDate(new java.sql.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
		assignment.setId(1);
		assignment.setName(TEST_ASSIGNMENT_NAME);
		assignment.setNeedsGrading(1);

		AssignmentGrade ag = new AssignmentGrade();
		ag.setAssignment(assignment);
		ag.setId(1);
		ag.setScore("80");
		ag.setStudentEnrollment(enrollment);

		// given -- stubs for database repositories that return test data
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));
		given(assignmentGradeRepository.findByAssignmentIdAndStudentEmail(1, TEST_STUDENT_EMAIL)).willReturn(ag);
		given(assignmentGradeRepository.findById(1)).willReturn(ag);

		// end of mock data

		// then do an http get request for assignment 1
		response = mvc.perform(MockMvcRequestBuilders.get("/gradebook/1").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify return data with entry for one student without no score
		assertEquals(200, response.getStatus());

		// verify that a save was NOT called on repository because student already has a
		// grade
		verify(assignmentGradeRepository, times(0)).save(any());

		// verify that returned data has non zero primary key
		GradebookDTO result = fromJsonString(response.getContentAsString(), GradebookDTO.class);
		// assignment id is 1
		assertEquals(1, result.assignmentId);
		// there is one student list
		assertEquals(1, result.grades.size());
		assertEquals(TEST_STUDENT_NAME, result.grades.get(0).name);
		assertEquals("80", result.grades.get(0).grade);

		// change grade to score = 88
		result.grades.get(0).grade = "88";

		// send updates to server
		response = mvc
				.perform(MockMvcRequestBuilders.put("/gradebook/1").accept(MediaType.APPLICATION_JSON)
						.content(asJsonString(result)).contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());

		// verify that repository save method was called
		// AssignmentGrade must override equals method for this test for work !!!
		AssignmentGrade updatedag = new AssignmentGrade();
		updatedag.setId(1);
		updatedag.setScore("88");
		verify(assignmentGradeRepository, times(1)).save(updatedag);
	}
	
	@Test
	public void createAssignment() throws Exception {
		MockHttpServletResponse response;
		
		String newAssignmentName = "New Assignment Name";
		
		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());
		
		given(courseRepository.findByCourse_id(TEST_COURSE_ID)).willReturn(course);
		
		// End MOCK DATA
		
		// SETUP NEW ASSIGNMENT SUBMISSION
		AssignmentListDTO.AssignmentDTO newA = new AssignmentListDTO.AssignmentDTO();
		newA.dueDate = "2021-11-01";
		newA.assignmentName = newAssignmentName;
		NewAssignmentFields newAFields = new NewAssignmentFields();
		newAFields.assignment = newA;
		newAFields.courseId = TEST_COURSE_ID;
		
		// send updates to server
		response = mvc
				.perform(MockMvcRequestBuilders.post("/assignment").accept(MediaType.APPLICATION_JSON)
						.content(asJsonString(newAFields)).contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();
		

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());
		
		// verify that returned data has non zero primary key
		AssignmentListDTO.AssignmentDTO result = fromJsonString(
				response.getContentAsString(),
				AssignmentListDTO.AssignmentDTO.class
		);
		
		assertEquals(newAssignmentName, result.assignmentName);
	}
	
	@Test
	public void deleteAssignment() throws Exception {
		MockHttpServletResponse response;
		
		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());
		
		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		course.getAssignments().add(assignment);
		// set dueDate to 1 week before now.
		assignment.setDueDate(new java.sql.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
		assignment.setId(1);
		assignment.setName(TEST_ASSIGNMENT_NAME);
		assignment.setNeedsGrading(1);
		
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));
		given(courseRepository.findByCourse_id(TEST_COURSE_ID)).willReturn(course);
		
		// End MOCK DATA
		
		// Get Assignment
		response = mvc
				.perform(MockMvcRequestBuilders.get("/assignment/1").accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());
		
		// Delete Assignment
		response = mvc
				.perform(MockMvcRequestBuilders.delete("/assignment/1").accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());
	}
	
	@Test
	public void deleteAssignmentWithGrades() throws Exception {
		MockHttpServletResponse response;
		
		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());
		
		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		course.getAssignments().add(assignment);
		// set dueDate to 1 week before now.
		assignment.setDueDate(new java.sql.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
		assignment.setId(1);
		assignment.setName(TEST_ASSIGNMENT_NAME);
		assignment.setNeedsGrading(1);
		
		Enrollment enrollment = new Enrollment();
		enrollment.setCourse(course);
		course.getEnrollments().add(enrollment);
		enrollment.setId(TEST_COURSE_ID);
		enrollment.setStudentEmail(TEST_STUDENT_EMAIL);
		enrollment.setStudentName(TEST_STUDENT_NAME);
		
		AssignmentGrade ag = new AssignmentGrade();
		ag.setAssignment(assignment);
		ag.setId(1);
		ag.setScore("80");
		ag.setStudentEnrollment(enrollment);
		
		given(assignmentGradeRepository.findByAssignmentIdAndStudentEmail(1, TEST_STUDENT_EMAIL)).willReturn(ag);
		List<AssignmentGrade> ags = new java.util.ArrayList<AssignmentGrade>();
		ags.add(ag);
		given(assignmentGradeRepository.findAllByAssignmentId(1)).willReturn(ags);
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));
		given(courseRepository.findByCourse_id(TEST_COURSE_ID)).willReturn(course);
		
		// End MOCK DATA
		
		// Get Assignment
		response = mvc
				.perform(MockMvcRequestBuilders.get("/assignment/1").accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());
		
		// Delete Assignment
		response = mvc
				.perform(MockMvcRequestBuilders.delete("/assignment/1").accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		

		// verify that return status = ERROR (value 400)
		// because there were grades for the assignment
		assertEquals(400, response.getStatus());
	}
	
	
	@Test
	public void updateAssignmentName() throws Exception {
		
		MockHttpServletResponse response;
		
		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());

		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		// set dueDate to 1 week before now.
		assignment.setDueDate(new java.sql.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
		assignment.setId(1);
		assignment.setName(TEST_ASSIGNMENT_NAME);

		// given -- stubs for database repositories that return test data
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));
		given(courseRepository.findByCourse_id(TEST_COURSE_ID)).willReturn(course);
		// End MOCK DATA
		
		/**
		 * VERIFY SETUP STATE
		 */
		response = mvc.perform(MockMvcRequestBuilders.get("/assignment/1").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify return data with entry for one student without no score
		assertEquals(200, response.getStatus());
		// verify that returned data has non zero primary key
		AssignmentListDTO.AssignmentDTO result = fromJsonString(
				response.getContentAsString(),
				AssignmentListDTO.AssignmentDTO.class
		);
		// assignment id is 1
		assertEquals(1, result.assignmentId);
		// there is one student list
		assertEquals(TEST_ASSIGNMENT_NAME, result.assignmentName);
		
		/**
		 * TEST UPDATE
		 */
		String updatedName = "Updated assignment name";
		AssignmentListDTO.AssignmentDTO assignmentUpdates = new AssignmentListDTO.AssignmentDTO();
		assignmentUpdates.assignmentName = updatedName;
		assignmentUpdates.assignmentId = 1;

		// send updates to server
		response = mvc
				.perform(MockMvcRequestBuilders.patch("/assignment/1").accept(MediaType.APPLICATION_JSON)
						.content(asJsonString(assignmentUpdates)).contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();
		

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());
	}

	private static String asJsonString(final Object obj) {
		try {

			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> T fromJsonString(String str, Class<T> valueType) {
		try {
			return new ObjectMapper().readValue(str, valueType);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
