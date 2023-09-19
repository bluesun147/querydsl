package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import static org.assertj.core.api.Assertions.assertThat;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;

// https://binco.tistory.com/entry/QueryDSL-조인-사용방법

@SpringBootTest
@Transactional
public class QueryDslTest {

	@Autowired
	EntityManager em;
	JPAQueryFactory queryFactory;

	// 테스트용 데이터 들어간 상황
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);

		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	// jpql 사용해 member 1 찾기
	// String 형식의 sql문 작성해야 하므로 런타임 시 오류 발생 확인 가능
	@Test
	public void startJPQL() {
		Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
				.setParameter("username", "member1")
				.getSingleResult();

		assertThat(findByJPQL.getUsername()).isEqualTo("member1");
	}

	/*
	Q 클래스 인스턴스 사용 방법 2가지
	별칭 직접 지정
	QMember m = new QMember("m");

	기본 인스턴스 사용 (같은 테이블 조인하지 않는 이상 기본 인스턴스 사용 추천)
	QMember m = QMember.member;
	*/

	// 쿼리 dsl은 jpql 빝더 역할을 하기 때문에 결론적으로 jpql 생성하여 조회
	// but 작접 작성과는 큰 차이. 직관적이고 파라미터 바인딩도 자동 처리
	// 쿼리에 오류 있다면 컴파일 시점에서 오류 발견 가능.
	@Test
	public void startQueryDsl() {
		Member findMember = queryFactory
				.select(member)
				.from(member)
				.where(member.username.eq("member1"))
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	// 조회
	@Test
	public void search() {
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1").and(member.age.eq(10)))
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
				.selectFrom(member)
				.where(
						member.username.eq("member1"), (member.age.eq(10))
				)
				.fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

}
