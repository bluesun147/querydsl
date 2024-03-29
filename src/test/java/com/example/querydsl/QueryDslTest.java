package com.example.querydsl;

import com.example.querydsl.dto.*;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;
import com.example.querydsl.repository.MemberRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;
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

	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = queryFactory
				.selectFrom(member)
				// age가 100인 회원들 중
				.where(member.age.eq(100))
				// 나이로 내림차순 -> 나이 같다면 이름으로 오름차순
				// nullsLast : 이름 없는 회원 맨 마지막에 출력
				.orderBy(member.age.desc(), member.username.asc().nullsLast())
				// fetch : 리스트 조회, 데이터 없으면 빈 리스트 반한
				// fetchOne : 하나 조회. 결과 2개 이상이면 NonUniqueResultException
				.fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();
	}


	// 페이징
	@Test
	public void paging() {
		List<Member> result = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				// offset으로 시작 index 알리고
				.offset(1)
				// limit으로 가져올 결과물 수 제한
				.limit(2)
				.fetch();

		for (Member member : result) {
			System.out.println("member = " + member.getUsername());
		}

		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void aggregation() {
		List<Tuple> result = queryFactory
				.select(member.count(),
						member.age.sum(),
						member.age.avg(),
						member.age.max(),
						member.age.min()
				)
				.from(member)
				.fetch();

		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	@Test
	public void group() {
		List<Tuple> result = queryFactory
				.select(team.name, member.age.avg())
				.from(member)
				.join(member.team, team)
				.groupBy(team.name)
				.fetch();

		Tuple teamA = result.get(0);
		System.out.println("!!!!!!! teamA.get(team.name) = " + teamA.get(team.name));
		System.out.println("!!!!!!! teamA.get(member.age.avg()) = " + teamA.get(member.age.avg()));

		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	// 기본 조인
	// 첫번째 파라미터에 조인 대상 저장, 두번째 파라미텅 별칭 (alias)
	// join(조인 대상, 별칭으로 사용할 Q 타입)
	@Test
	public void join() {
		List<Member> result = queryFactory
				.selectFrom(member)
				.join(member.team, team)
				.where(team.name.eq("teamA"))
				.fetch();

		assertThat(result)
				.extracting("username")
				.containsExactly("member1", "member2");
	}

	// @Autowired와 다른점은 스프링 의존도가 없는 순수한 JPA 코드라는 점
	// javaEE 컨테이너에서 EntityManagerFactory 제공받아 사용하는 전형적인 JPA 코드
	// https://milenote.tistory.com/171
	@PersistenceUnit
	EntityManagerFactory emf;
	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();

		Member findMember = queryFactory
				.selectFrom(member)
				//fetch join 없음
				.where(member.username.eq("member1"))
				.fetchOne();

		// team 필드를 lazy로 설정해두면 member.getTeam 호출하지 않는 이상
		// team을 조회하는 select문이 db에 전달되지 않음.

		// 해당 엔티티가 로딩되어있는지 확인시켜주는 기능
		// mem1 이라는 이름 가진 유저 찾고 그 유저로 getTeam() 호출 했을 때 로딩 되었는지 테스트 하는 코드
		// getTeam 해도 아무것도 없다는 말
		boolean loaded =
				emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("패치 조인 미적용").isFalse();
	}

	// fetch join 사용
	@Test
	public void fetchJoinUse() {
		em.flush();
		em.clear();

		Member findMember = queryFactory
				.selectFrom(member)
				.join(member.team, team).fetchJoin()
				.where(member.username.eq("member1"))
				.fetchOne();

		boolean loaded =
				emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("패치 조인 적용").isTrue();
	}

	// 프로젝션 대상이 하나인 경우
	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
				.select(member.username)
				.from(member)
				.fetch();

		for(String s : result) {
			System.out.println("s = " + s);
		}
	}

	// jqpl 활용한 DTO 반환 방법
	// tuple 말고 dto를 사용하자 -> db 노출 위험
	// jpql 사용해서 dto 반환받으려면 new 연산자 사용해야 함.
	// 코드 지저분하고 생성자 방식만 지원
	// but 쿼리 dsl은 프로퍼티, 필드 직접 접근, 생성자 사용 -> 3가지 지원
	@Test
	public void findDtoByJpql() {
		List<MemberDto> resultList =
				em.createQuery("select new com.example.querydsl.dto.MemberDto(m.username, m.age) " +
						"from Member m", MemberDto.class).getResultList();

		for (MemberDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	// queryDsl 활용한 DTO 반환 방법

	// 1. 프로퍼티(Setter) 활용해 DTO 반환받는 방법
	// Projections의 bean() 메소드 호출해 매핑할 클래스와 매핑할 필드 순서대로 전달
	@Test
	public void findDtoBySetter() {
		List<MemberDto> result = queryFactory
				.select(Projections.bean(MemberDto.class,
						member.username,
						member.age))
				.from(member)
				.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("!!!!! memberDto = " + memberDto);
		}
	}

	// 2. 필드 직접 접근 방법
	// bean 코드에서 fields()로 방식만 수정해주면 됨
	// 생성자 방식도 constructor로만 바꾸면 됨
	// fields()는 bean과 다르게 getter,setter 필요없고 결과는 동일
	@Test
	public void findDtoByField() {
		List<MemberDto> result = queryFactory
				.select(Projections.fields(MemberDto.class,
//				.select(Projections.constructor(MemberDto.class,
						member.username,
						member.age))
				.from(member)
				.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("!!!!! memberDto = " + memberDto);
		}
	}

	// 필드명이 다른 DTO 사용해서 매핑해야 할 경우 (username // name)
	// 엔티티와 dto 필드명 다를 때
	@Test
	public void findUserDto() {
		List<UserDto> result = queryFactory
				.select(Projections.fields(UserDto.class,
						// 별칭 메서드 사용
						member.username.as("name"),
						member.age))
				.from(member)
				.fetch();

		for (UserDto userDto : result) {
			System.out.println("userDto = " + userDto);
		}
	}

	/*
	@QueryProjection 활용 하면 컴파일러로 타입 체크 가능
	-> dto 생성자에 어노테이션 삽입해 사용
	컴파일 단계에서 오류 찾기 가능 -> 가장 안전한 방법
	but dto에 queryDsl 어노테이션 유지해야 한다는 점과 dto까지 Q파일 생성해야 한다는 단점 존재
	 */
	@Test
	public void findByQueryProjection() {
		List<MemberDto> result = queryFactory
				.select(new QMemberDto(member.username, member.age))
				.from(member)
				.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("!!!!! memberDto = " + memberDto);
		}
	}

	// 동적 쿼리
	// 동적 쿼리 BooleanBuilder 사용
	// 쿼리의 조건 설정인 where 뒤의 조건 생성해주는 것
	// 원하는 조건을 다 넣은 후 queryFactory.where 절에 해당 builder를 넣어주면 조건문 완성
	private List<Member> searchMember(String usernameCond, Integer ageCond) {
		BooleanBuilder builder = new BooleanBuilder();
		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}

		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}

		return queryFactory
				.selectFrom(member)
				.where(builder)
				.fetch();
	}

	// 원하는 조건을 다 넣은 뒤 queryFactory.where 절에 해당 builder를 넣으면 조건문 완성
	@Test
	public void dynamicQuery_BooleanBuilder() {

		String userParam = "member1";
		Integer ageParam = 10;

		// 파라미터로 넣기
		List<Member> result = searchMember(userParam, ageParam);

		// 1명 밖에 없음
		assertThat(result.size()).isEqualTo(1);
	}

	// where 다중 파라미터 사용하면 메서드들을 다른 쿼리에서도 재활용 가능, 쿼리 자체의 가독성 높아짐
	@Test
	public void dynamicQuery_WhereParam() {
		String usernameParam = "member1";
		Integer ageParam = 10;

		List<Member> result = searchMemberWithWhere(usernameParam, ageParam);

		for (Member memberX : result) {
			System.out.println("!!!!! memberX = " + memberX);
		}

		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMemberWithWhere(String usernameCond, Integer ageCond) {
		return queryFactory
				.selectFrom(member)
				.where(usernameEq(usernameCond), ageEq(ageCond))
//				.where(allEq(usernameCond, ageCond))
				.fetch();
	}

	private BooleanExpression usernameEq(String usernameCond) {
		if (usernameCond == null) {
			return null;
		}
		return member.username.eq(usernameCond);
	}

	private BooleanExpression ageEq(Integer ageCond) {
		if (ageCond == null) {
			return null;
		}
		return member.age.eq(ageCond);
	}

	private BooleanExpression allEq(String usernameCond, Integer ageCond) {
		return usernameEq(usernameCond).and(ageEq(ageCond));
	}

	// 리턴값이 null 일 때 쿼리dsl은 자동으로 해당 값 무시하므로 동적 쿼리 존재

	// jpa에는 update 따로 메소드 없고 dirty checking(변경 감지) 통해 수정 진행
	// 더티 체킹은 건별로 업데이트 진행하기 때문에 한번에 업데이트하는 로직은 bulk 연산 사용이 좋다


	// 28살보다 작은 회원들의 이름을 "비회원"으로 수정
	@Test
	public void bulkUpdate() {
		long count = queryFactory
				.update(member)
				.set(member.username, "비회원")
				.where(member.age.lt(28))
				.execute();

		// jpa는 기본적으로 트랜잭션 끝나기 전까지 영속성 컨텍스트에 쿼리문 쌓아 놓았다가 끝남과 동시에 DB에 전달
		// 하지만 벌크 연산은 db에 바로 전달
		em.flush();
		em.clear();
	}

	/*
	업데이트 후 다시 db에서 조회하는 로직 추가된다면?
	여전히 바뀌지 않음 -> 영속성 컨텍스트와 db값 불일치 현상 때문.
	벌크 연산 통해 db값은 바로 바뀌고, 영속성 컨텍스트는 이전의 값 남아있는 불일치 현상 해결 위해
	영속성 컨텍스트 모두 초기화 시켜주는 em.flush()와 em.clear() 들어간 이유
	 */

	// 모든 회원의 나이 1씩 더하는 로직
	// 빼기는 -1 넣으면 됨
	@Test
	public void bulkAdd() {
		long count = queryFactory
				.update(member)
				.set(member.age, member.age.add(1))
				.execute();
	}

	@Test
	public void bulkDelete() {
		long count = queryFactory
				.delete(member)
				.where(member.age.gt(18))
				.execute();
	}

	// 리포지토리 적용
	@Autowired
	MemberRepository memberRepository;

	@Test
	public void repositoryTest() {
		Member member = new Member("member3", 60);
		memberRepository.save(member);

//		List<Member> result = memberRepository.findAll_queryDsl();
//		assertThat(result).containsExactly(member);

		List<Member> result2 = memberRepository.findByname_queryDsl("member3");
		for (Member m : result2) {
			System.out.println("!!!!!!!!!!!!!!!!!!!!!! m = " + m);
		}
		assertThat(result2).contains(member);
	}

	// build, where 패턴 테스트
	@Test
	public void searchTest() {
		MemberSearchCondition condition = new MemberSearchCondition();
		condition.setAgeGoe(35);
		condition.setAgeLoe(40);
		condition.setTeamName("teamB");

		List<MemberTeamDto> result =
				memberRepository.searchByBuilder(condition);

		System.out.println("!!!!!!!!!!!!!! result.size is " + result.size());
		for (MemberTeamDto m : result) {
			System.out.println("!!!!!!!!!!!!!! m = " + m);
		}

		assertThat(result).extracting("username").contains("member4");
	}
}