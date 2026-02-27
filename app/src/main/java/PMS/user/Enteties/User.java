package PMS.user.Enteties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index( name ="idx_email", columnList = "email")
}) 
@Getter
@Setter
@NoArgsConstructor
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    
    private String password;
    private String name;
    private String[] roles;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean active;

    public User(String email, String password, String name, String[] roles, boolean active) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.roles = roles;
        this.active = active;
    }


}
