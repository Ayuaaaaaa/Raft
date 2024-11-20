package com.github.raftimpl.raft.service;

import com.baidu.brpc.client.RpcCallback;
import com.github.raftimpl.raft.proto.RaftProto;

import java.util.concurrent.Future;

/**
 * 用于生成client异步调用所需的proxy
 * Created by raftimpl on 2017/5/2.
 */
public interface RaftConsensusServiceAsync extends RaftConsensusService {

    Future<RaftProto.VoteResponse> preVote(
            RaftProto.VoteRequest request,
            RpcCallback<RaftProto.VoteResponse> callback);

    Future<RaftProto.VoteResponse> requestVote( //raft consensus
            RaftProto.VoteRequest request,
            RpcCallback<RaftProto.VoteResponse> callback);

    Future<RaftProto.AppendEntriesResponse> appendEntries( //复制日志
            RaftProto.AppendEntriesRequest request,
            RpcCallback<RaftProto.AppendEntriesResponse> callback);

    Future<RaftProto.InstallSnapshotResponse> installSnapshot( //安装快照
            RaftProto.InstallSnapshotRequest request,
            RpcCallback<RaftProto.InstallSnapshotResponse> callback);
}
