package com.cst438.domain;

import java.util.ArrayList;

/*
 * a transfer object that is a list of assignment grades
 */
public class AssignmentGradeListDTO {
	
	public AssignmentGradeListDTO() {};
	
	public static class AssignmentGradeDTO {
		public String assignmentName;
		public int assignmentGradeId;
		public int courseId;
		public String courseTitle;
		public String dueDate;
		public String score;
		
		public AssignmentGradeDTO() {};
		
		public AssignmentGradeDTO(AssignmentGrade ag) {
			this.assignmentName = ag.getAssignment().getName();
			this.assignmentGradeId = ag.getId();
			this.dueDate = ag.getAssignment().getDueDate().toString();
			this.score = ag.getScore();
			this.courseTitle = ag.getAssignment().getCourse().getTitle();
		}
	}
	
	public ArrayList<AssignmentGradeDTO> assignmentGrades = new ArrayList<>();

}
