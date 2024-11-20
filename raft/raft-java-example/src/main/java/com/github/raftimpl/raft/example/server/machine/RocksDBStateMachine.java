package com.github.raftimpl.raft.example.server.machine;

import btree4j.BTreeException;
import com.github.raftimpl.raft.RaftNode;
import com.github.raftimpl.raft.StateMachine;
import com.github.raftimpl.raft.example.server.service.ExampleProto;
import com.github.raftimpl.raft.proto.RaftProto;
import com.github.raftimpl.raft.storage.SegmentedLog;
import org.apache.commons.io.FileUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.rocksdb.Checkpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 基于Rocksdb的状态机实现
 */
public class RocksDBStateMachine implements StateMachine {
    // log记录日志，
    private static final Logger LOG = LoggerFactory.getLogger(RocksDBStateMachine.class);
    // 静态代码块，项目启动时执行，用于初始化RocksDB，加载RocksDB依赖包
    static {
        RocksDB.loadLibrary();
    }

    private RocksDB db;// RocksDB数据库

    private final String raftDataDir;   // 节点数据存放路径
    // 构造方法，从外部传入数据存放路径
    public RocksDBStateMachine(String raftDataDir) {
        this.raftDataDir = raftDataDir;
    }

    /**
     * StateMachine接口中的方法重写，实现snapShot写入
     * @param snapshotDir 旧snapshot目录
     * @param tmpSnapshotDataDir 新snapshot数据目录
     * @param raftNode Raft节点
     * @param localLastAppliedIndex 已应用到复制状态机的最大日志条目索引
     */
    @Override
    public void writeSnapshot(String snapshotDir, String tmpSnapshotDataDir, RaftNode raftNode, long localLastAppliedIndex) {
        // 构造RocksDB的Checkpoint
        Checkpoint checkpoint = Checkpoint.create(db);
        try {
            checkpoint.createCheckpoint(snapshotDir);
            LOG.info("Checkpoint was created");
        } catch (Exception e) {
            LOG.warn("writeSnapshot meet exception, dir={}, msg={}",
                    snapshotDir, e.getMessage());
        }
    }

    /**
     * StateMachine接口中的方法重写，实现snapShot读取
     * @param snapshotDir snapshot数据目录
     */
    @Override
    public void readSnapshot(String snapshotDir) {
        try {
            // 因为要做文件夹复制，所以先关闭数据库连接
            if (db != null) {
                db.close();
                db = null;
            }
            // 节点存放rocksdb data路径
            String dataDir = raftDataDir + File.separator + "rocksdb_data";
            File dataFile = new File(dataDir);
            if (dataFile.exists()) {
                FileUtils.deleteDirectory(dataFile);
            }
            File snapshotFile = new File(snapshotDir);
            // 将snapshot下的文件复制到dataDir下
            if (snapshotFile.exists()) {
                FileUtils.copyDirectory(snapshotFile, dataFile);
            }
            // 重新打开数据库连接
            Options options = new Options();
            options.setCreateIfMissing(true);
            db = RocksDB.open(options, dataDir);
        } catch (Exception e) {
            LOG.warn("meet exception, msg={}", e.getMessage());
        }
        LOG.info("snapshot has been copied");
    }

    /**
     * StateMachine接口中的方法重写，实现状态机数据同步
     * @param dataBytes 数据二进制
     */
    @Override
    public void apply(byte[] dataBytes) {
        try {
            ExampleProto.SetRequest request = ExampleProto.SetRequest.parseFrom(dataBytes);
            db.put(request.getKey().getBytes(), request.getValue().getBytes());
            LOG.info("data has been applied in RocksDB");
        } catch (Exception e) {
            LOG.warn("meet exception, msg={}", e.getMessage());
        }
    }


    /**
     * StateMachine接口中的方法重写，实现状态机数据获取
     * @param dataBytes Key的数据二进制
     * @return
     */
    @Override
    public byte[] get(byte[] dataBytes) {
        byte[] result = null;
        try {
            byte[] valueBytes = db.get(dataBytes);
            if (valueBytes != null) {
                result = valueBytes;
            }
        } catch (Exception e) {
            LOG.warn("read rocksdb error, msg={}", e.getMessage());
        }
        LOG.info("data has been getted");
        LOG.info("the key is:"+new String(dataBytes));
        LOG.info("the value is:"+new String(result));
        return result;
    }

}

