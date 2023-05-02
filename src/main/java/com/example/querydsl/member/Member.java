package com.example.querydsl.member;

import com.example.querydsl.team.Team;
import lombok.*;

import javax.persistence.*;

/*
build/generated/querydsl에 Q클래스가 생성된 것을 확인할 수 있다
querydsl 이 q-class 사용하는 이유는?
MetaClass인 Q-Class 라는것을 만드는 이유 :

Querydsl은 컴파일 시점에 타입 안전성을 보장하기 위해 Q 클래스를 생성함
EntityClass에서 property에 접근하려면 객체를 만들어 접근해야 함.
-> MetaClass 즉 static한 Class를 만들어 지원을 하게되면, 객체를 생성할 필요없이 static이므로 property에 바로 접근하여 사용할 수 있게된다.
그래서 QueryDSL을 사용할 때 해당 Entity클래스의 propery에 접근을 바로 할 수 있었던 것!
 */

@Entity
@Getter
@Setter // 실무에서는 entity에 사용x! 실제로는 dto에서
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this(username, 0, null);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}