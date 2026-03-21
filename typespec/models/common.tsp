namespace AuthService;

model SuccessResponse<T> {
  status: int32;
  statusMessage: string;
  message: string;
  data: T;
  timestamp: utcDateTime;
}

@error
model ErrorResponse {
  status: int32;
  statusMessage: string;
  message: string;
  path: string;
  method: string;
  timestamp: utcDateTime;

  fieldErrors?: Record<string>;
  rejectedValues?: Record<unknown>;
  details?: Record<unknown>;
}