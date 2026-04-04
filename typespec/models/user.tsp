namespace AuthService;

model User {
  id: uuid;
  username: string;
  email: string;
  roles: Roles[];
  emailVerified: boolean;
  accountStatus: AccountStatus;
}


enum Roles{
    USER,
    ADMIN
}

enum AccountStatus{
    ACTIVE,
    BLOCKED,
    DELETED
}

@format("uuid")
scalar uuid extends string;