package com.mjsec.lms.service;

import com.mjsec.lms.domain.Attendance;
import com.mjsec.lms.domain.StudyActivity;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.StudyActivityDto;
import com.mjsec.lms.dto.StudyActivityResponse;
import com.mjsec.lms.dto.StudyAttendanceDto;
import com.mjsec.lms.repository.AttendanceRepository;
import com.mjsec.lms.repository.StudyActivityRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final ValidationUtils validationUtils;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;

    StudyGroupService(StudyGroupRepository studyGroupRepository,
                      ValidationUtils validationUtils,
                      StudyActivityRepository studyActivityRepository,
                      UserRepository userRepository,
                      AttendanceRepository attendanceRepository){

        this.studyGroupRepository = studyGroupRepository;
        this.validationUtils = validationUtils;
        this.studyActivityRepository = studyActivityRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
    }

    //스터디 활동 글 + 출석체크 설정하기
    public StudyActivityResponse createStudyActivity(Long groupId, Long currentUserStudentNumber, StudyActivityDto studyActivityDto){

        log.info("createStudyActivity called!");

        //검증 로직들 (사용자, 스터디그룹, 멘토 조건)
        User user = validationUtils.validateBasicAccess(groupId,currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);

        //스터디 활동 글 생성
        StudyActivity savedStudyActivity = createStudyActivityData(groupId, studyActivityDto);

        //출석체크 리스트 생성
        List<Attendance> attendanceList = createAttendanceListData(savedStudyActivity, studyActivityDto);

        //활동글 Response 생성 후 반환
        return createStudyActivityResponse(savedStudyActivity,attendanceList);
    }


    /*
    DTO 생성용 메서드
     */

    //Dto를 받아서 StudyActivity 저장하기
    public StudyActivity createStudyActivityData(Long groupId, StudyActivityDto studyActivityDto){

        StudyActivity studyActivity = StudyActivity.builder()
                .title(studyActivityDto.getTitle())
                .content(studyActivityDto.getContent())
                .studyGroup(studyGroupRepository.findById(groupId).orElseThrow())
                .build();

        return studyActivityRepository.save(studyActivity);
    }

    //Dto와 StudyActivity 객체를 받아 출석체크 리스트를 저장하기
    public List<Attendance> createAttendanceListData(StudyActivity savedStudyActivity, StudyActivityDto dto){

        List<Attendance> attendances = dto.getStudyAttendanceDtoList().stream()
                .map(attendanceDto -> Attendance.builder()
                        .user(userRepository.findByStudentNumber(attendanceDto.getStudentNumber()).orElseThrow())
                        .studyGroup(savedStudyActivity.getStudyGroup())
                        .studyActivity(savedStudyActivity)
                        .type(attendanceDto.getType())
                        .attendanceDate(LocalDate.now())
                        .build())
                .collect(Collectors.toList());

        return attendanceRepository.saveAll(attendances);
    }

    public StudyActivityResponse createStudyActivityResponse(StudyActivity studyActivity, List<Attendance> attendanceList){

        List<StudyAttendanceDto> studyAttendanceDtoList = attendanceList.stream()
                .map(attendance -> StudyAttendanceDto.builder()
                        .studentNumber(attendance.getUser().getStudentNumber())
                        .name(attendance.getUser().getName())
                        .type(attendance.getType())
                        .build())
                .toList();

        return StudyActivityResponse.builder()
                .StudyActivityId(studyActivity.getActivityId())
                .title(studyActivity.getTitle())
                .content(studyActivity.getContent())
                .studyAttendanceDtoList(studyAttendanceDtoList)
                .createdAt(studyActivity.getCreatedAt())
                .build();
    }


}
