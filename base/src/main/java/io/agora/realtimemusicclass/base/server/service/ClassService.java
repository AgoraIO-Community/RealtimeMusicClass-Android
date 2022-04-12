package io.agora.realtimemusicclass.base.server.service;

import java.util.List;

import io.agora.realtimemusicclass.base.server.struct.ServerResponseBody;
import io.agora.realtimemusicclass.base.server.struct.body.ClassCreateBody;
import io.agora.realtimemusicclass.base.server.struct.body.ClassJoinBody;
import io.agora.realtimemusicclass.base.server.struct.body.UserUpdateBody;
import io.agora.realtimemusicclass.base.server.struct.response.ClassBriefInfoResp;
import io.agora.realtimemusicclass.base.server.struct.response.ClassJoinResp;
import io.agora.realtimemusicclass.base.server.struct.response.UserInfoResp;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ClassService {
    @POST("/room/create")
    Call<ServerResponseBody<ClassBriefInfoResp>> createClass(@Body ClassCreateBody body);

    @DELETE("/room/{className}")
    Call<ServerResponseBody<String>> deleteClass(@Path("className") String className);

    @GET("/room/info/{className}")
    Call<ServerResponseBody<ClassBriefInfoResp>> getClassInfo(@Path("className") String className);

    @GET("/room/list/{pageNo}")
    Call<ServerResponseBody<List<ClassBriefInfoResp>>> getClassList(@Path("pageNo") int pageNo);

    @GET("/room/my/{creator}")
    Call<ServerResponseBody<List<ClassBriefInfoResp>>> getClassListByCreator(@Path("creator") String creator);

    @POST("/room/enter/{className}")
    Call<ServerResponseBody<ClassJoinResp>> joinClass(@Path("className") String className,
                                                      @Body ClassJoinBody body);

    @PUT("/room/{className}/user/{userName}")
    Call<ServerResponseBody<String>> heartbeat(@Path("className") String className,
                                               @Path("userName") String userName);

    @GET("/room/exit/{className}/{userName}")
    Call<ServerResponseBody<String>> leaveClass(@Path("className") String className,
                                                @Path("userName") String userName);

    @GET("/room/{className}/users/{userType}")
    Call<ServerResponseBody<List<UserInfoResp>>> getClassUserListByRole(@Path("className") String className,
                                                                        @Path("userType") String userType);

    @GET("/room/{className}/users")
    Call<ServerResponseBody<List<UserInfoResp>>> getClassFullUserList(@Path("className") String className);

    @GET("/room/{className}/user/{userName}")
    Call<ServerResponseBody<UserInfoResp>> getClassUserInfo(@Path("className") String className,
                                                       @Path("userName") String userName);

    @POST("/room/{className}/user/{userName}")
    Call<ServerResponseBody<String>> updateUserInfo(@Path("className") String className,
                                                    @Path("userName") String userName,
                                                    @Body UserUpdateBody body);
}
