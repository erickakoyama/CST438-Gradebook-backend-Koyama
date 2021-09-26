package com.cst438.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentDTO;
import com.cst438.domain.EnrollmentRepository;

@RestController
public class EnrollmentController {

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	EnrollmentRepository enrollmentRepository;

	/*
	 * endpoint used by registration service to add an enrollment to an existing
	 * course.
	 */
	@PostMapping("/enrollment")
	@Transactional
	public EnrollmentDTO addEnrollment(@RequestBody EnrollmentDTO enrollmentDTO) {
		/**
		 * this class receives a request to create an enrollment record for a
		 * student in a course using an EnrollmentDTO object as the request body.
		 */
		Enrollment e = new Enrollment();
		Course c = courseRepository.findByCourse_id(enrollmentDTO.course_id);
		
		if (c != null) {
			e.setCourse(c);
			e.setStudentEmail(enrollmentDTO.studentEmail);
			e.setStudentName(enrollmentDTO.studentName);
			
			enrollmentRepository.save(e); // save to DB
		} else {
			throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "Could not find course by id: " + enrollmentDTO.course_id );
		}
		
		return enrollmentDTO;
	}

}
