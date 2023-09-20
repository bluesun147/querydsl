package com.example.querydsl.repository;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.dto.QMemberTeamDto;
import com.example.querydsl.entity.Member;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;

@Repository
public class MemberRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    // Optional로 반환하는 이유는 memberId에 해당하는 Member 값을 이용해 Member Entity가 없을 수도 있기 때문
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);

        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll_jpql() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_queryDsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }


    // 유저 이름으로 조회
    // jpql 사용
    // 쿼리문에 직접 세팅해야 함
    public List<Member> findByUsername_jpql(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    // query dsl 사용
    // where 절 이용해서 간단하게 구현 가능
    // 파라미터 많아지면 가독성 올라감.
    public List<Member> findByname_queryDsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    // builder 패턴
    // builder.and는 동적으로 조건 추가하는데 사용되는 메소드
    // jpql 보다 가독성 좋지만 해당 builder 다른 곳에서 재사용 불가
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        // 여러 조건을 논리적으로 결합하기 위한 컨테이너 역할
        // 빌더에 조건들을 추가
        BooleanBuilder builder = new BooleanBuilder();
        // StringUtils.hasText : 값이 있으면 true, 공백이나 null일 경우 false 반환
        if (StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.goe(condition.getAgeLoe()));
        }

        /*
        left join -> 왼쪽 테이블 중심으로 오른쪽 테이블 매치시킴
        왼쪽 테이블의 레코드 하나에 오른쪽 테이블 레코드 여러개 일치할 경우
        해당 왼쪽 레코드 여러번 표시
        왼쪽은 무조건 표시하고 매치되는 레코드 오른쪽에 없으면 null 표시
        밑에서는 member.team을 기준으로 테이블 생성
         */
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    // where 패턴
    // where 패턴의 장점은 여러 메소드로 나눠서 각각의 메서드를 혼합해 사용할 수 있고, 재사용성 높아짐
    public List<MemberTeamDto> searchByWhere(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age, //
                        member.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        if (StringUtils.hasText(username)) {
            return member.username.eq(username);
        }
        return null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        if (StringUtils.hasText(teamName)) {
            return team.name.eq(teamName);
        }
        return null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        if (ageGoe != null) {
            // goe, loe는 쿼리 dsl 내장 메소드
            return member.age.goe(ageGoe);
        }
        return null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        if (ageLoe != null) {
            return member.age.loe(ageLoe);
        }
        return null;
    }
}