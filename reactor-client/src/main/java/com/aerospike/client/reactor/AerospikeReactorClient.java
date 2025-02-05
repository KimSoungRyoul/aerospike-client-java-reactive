/*
 * Copyright 2012-2018 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.client.reactor;

import com.aerospike.client.*;
import com.aerospike.client.async.AsyncIndexTask;
import com.aerospike.client.cdt.CTX;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.*;
import com.aerospike.client.query.IndexCollectionType;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.Statement;
import com.aerospike.client.reactor.dto.KeyExists;
import com.aerospike.client.reactor.dto.KeyObject;
import com.aerospike.client.reactor.dto.KeysExists;
import com.aerospike.client.reactor.dto.KeysRecords;
import com.aerospike.client.reactor.listeners.*;
import com.aerospike.client.task.Task;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * Instantiate an <code>AerospikeReactorClient</code> object to access an Aerospike
 * database cluster and perform database operations reactively.
 * <p>
 * This client is just wrapper over AerospikeClient async methods that provides Reactor interface
 * <p>
 * This client is thread-safe. One client instance should be used per cluster.
 * Multiple threads should share this cluster instance.
 * <p>
 * Your application uses this class API to perform database operations such as
 * writing and reading records, and selecting sets of records. Write operations
 * include specialized functionality such as append/prepend and arithmetic
 * addition.
 * <p>
 * Each record may have multiple bins, unless the Aerospike server nodes are
 * configured as "single-bin". In "multi-bin" mode, partial records may be
 * written or read by specifying the relevant subset of bins.
 *
 * @author Sergii Karpenko
 * @author Eugene Rizhkov
 */
public class AerospikeReactorClient implements IAerospikeReactorClient{

	private final IAerospikeClient aerospikeClient;

	/**
	 * @param aerospikeClient the {@link com.aerospike.client.AerospikeClient} instance
	 */
	public AerospikeReactorClient(IAerospikeClient aerospikeClient) {
		this.aerospikeClient = aerospikeClient;
	}

	@Override
	public void close(){
		aerospikeClient.close();
	}

	@Override
	public final Mono<KeyRecord> get(Key key) throws AerospikeException {
		return get(null, key);
	}

	@Override
	public final Mono<KeyRecord> get(Policy policy, Key key) throws AerospikeException {
		return get(policy, key, null);
	}

	@Override
	public final Mono<KeyRecord> get(Policy policy, Key key, String[] binNames) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.get(
				null, new ReactorRecordListener(sink), policy, key, binNames));
	}

	@Override
	public final Mono<KeysRecords> get(Key[] keys) throws AerospikeException {
		return get(null, keys);
	}

	@Override
	public final Mono<KeysRecords> get(BatchPolicy policy, Key[] keys) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.get(
				null, new ReactorRecordArrayListener(sink), policy, keys));
	}

	@Override
	public final Mono<List<BatchRead>> get(List<BatchRead> records) throws AerospikeException {
		return get(null, records);
	}

	@Override
	public final Mono<List<BatchRead>> get(BatchPolicy policy, List<BatchRead> records) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.get(
				null, new ReactorBatchListListener(sink), policy, records));
	}

	@Override
	public final Mono<KeysRecords> get(Key[] keys, Operation... operations) throws AerospikeException {
		return get(null, keys, operations);
	}

	@Override
	public final Mono<KeysRecords> get(BatchPolicy policy, Key[] keys, Operation... operations) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.get(
				null, new ReactorRecordArrayListener(sink), policy, keys, operations));
	}

	@Override
	public final Flux<BatchRead> getFlux(List<BatchRead> records) throws AerospikeException {
		return getFlux(null, records);
	}

	@Override
	public final Flux<BatchRead> getFlux(BatchPolicy policy, List<BatchRead> records) throws AerospikeException {
		return Flux.create(sink -> aerospikeClient.get(
				null, new ReactorBatchSequenceListener(sink), policy, records));
	}

	@Override
	public final Flux<KeyRecord> getFlux(Key[] keys) throws AerospikeException {
		return getFlux(null, keys);
	}

	@Override
	public final Flux<KeyRecord> getFlux(BatchPolicy policy, Key[] keys) throws AerospikeException {
		return Flux.create(sink -> aerospikeClient.get(
				null, new ReactorRecordSequenceListener(sink), policy, keys));
	}

	@Override
	public final Flux<KeyRecord> getFlux(Key[] keys, Operation... operations) throws AerospikeException {
		return getFlux(null, keys, operations);
	}

	@Override
	public final Flux<KeyRecord> getFlux(BatchPolicy policy, Key[] keys, Operation... operations) throws AerospikeException {
		return Flux.create(sink -> aerospikeClient.get(
				null, new ReactorRecordSequenceListener(sink), policy, keys, operations));
	}

	@Override
	public final Mono<KeyRecord> getHeader(Key key) throws AerospikeException {
		return getHeader(null, key);
	}

	@Override
	public final Mono<KeyRecord> getHeader(Policy policy, Key key) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.getHeader(
				null, new ReactorRecordListener(sink), policy, key));
	}

	@Override
	public final Mono<KeysRecords> getHeaders(Key[] keys) throws AerospikeException {
		return getHeaders(null, keys);
	}

	@Override
	public final Mono<KeysRecords> getHeaders(BatchPolicy policy, Key[] keys) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.getHeader(
				null, new ReactorRecordArrayListener(sink),
				policy, keys));
	}

	@Override
	public final Mono<Key> touch(Key key) throws AerospikeException {
		return touch(null, key);
	}

	@Override
	public final Mono<Key> touch(WritePolicy policy, Key key) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.touch(
				null, new ReactorWriteListener(sink), policy, key));
	}

	@Override
	public final Mono<Key> exists(Key key) throws AerospikeException {
		return exists(null, key);
	}

	@Override
	public final Mono<Key> exists(Policy policy, Key key) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.exists(
				null, new ReactorExistsListener(sink), policy, key));
	}

	@Override
	public final Mono<KeysExists> exists(Key[] keys) throws AerospikeException {
		return exists(null, keys);
	}

	@Override
	public final Mono<KeysExists> exists(BatchPolicy policy, Key[] keys) throws AerospikeException{
		return Mono.create(sink -> aerospikeClient.exists(
				null, new ReactorExistsArrayListener(sink), policy, keys));
	}

	@Override
	public final Flux<KeyExists> existsFlux(Key[] keys) throws AerospikeException {
		return existsFlux(null, keys);
	}

	@Override
	public final Flux<KeyExists> existsFlux(BatchPolicy policy, Key[] keys) throws AerospikeException {
		return Flux.create(sink -> aerospikeClient.exists(
				null, new ReactorExistsSequenceListener(sink), policy, keys));
	}

	@Override
	public final Mono<Key> put(Key key, Bin... bins) throws AerospikeException {
		return put(null, key, bins);
	}

	@Override
	public final Mono<Key> put(WritePolicy policy, Key key, Bin... bins) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.put(
				null, new ReactorWriteListener(sink), policy, key, bins));
	}

	@Override
	public final Mono<Key> append(Key key, Bin... bins) throws AerospikeException {
		return append(null, key, bins);
	}

	@Override
	public final Mono<Key> append(WritePolicy policy, Key key, Bin... bins) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.append(
				null, new ReactorWriteListener(sink), policy, key, bins));
	}

	@Override
	public final Mono<Key> prepend(Key key, Bin... bins) throws AerospikeException {
		return prepend(null, key, bins);
	}

	@Override
	public final Mono<Key> prepend(WritePolicy policy, Key key, Bin... bins) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.prepend(
				null, new ReactorWriteListener(sink), policy, key, bins));
	}

	@Override
	public final Mono<Key> add(Key key, Bin... bins) throws AerospikeException {
		return add(null, key, bins);
	}

	@Override
	public final Mono<Key> add(WritePolicy policy, Key key, Bin... bins) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.add(
				null, new ReactorWriteListener(sink), policy, key, bins));
	}

	@Override
	public final Mono<Key> delete(Key key) throws AerospikeException {
		return delete(null, key);
	}

	@Override
	public final Mono<Key> delete(WritePolicy policy, Key key) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.delete(
				null, new ReactorDeleteListener(sink), policy, key));
	}

	@Override
	public Mono<BatchResults> delete(BatchPolicy batchPolicy, BatchDeletePolicy deletePolicy,
									 Key[] keys) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.delete(
				null, new ReactorBatchRecordArrayListener(sink), batchPolicy, deletePolicy, keys));
	}

	@Override
	public final Mono<KeyRecord> operate(Key key, Operation... operations) throws AerospikeException {
		return operate(null, key, operations);
	}

	@Override
	public final Mono<KeyRecord> operate(WritePolicy policy, Key key, Operation... operations) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.operate(
				null, new ReactorRecordListener(sink), policy, key, operations));
	}

	@Override
	public Mono<BatchResults> operate(BatchPolicy batchPolicy, BatchWritePolicy writePolicy, Key[] keys,
									  Operation... ops) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.operate(
				null, new ReactorBatchRecordArrayListener(sink), batchPolicy, writePolicy, keys, ops));
	}

	@Override
	public Mono<Boolean> operate(BatchPolicy policy, List<BatchRecord> records) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.operate(
				null, new ReactorBatchOperateListListener(sink), policy, records));
	}

	@Override
	public Mono<CommitStatus> commit(Txn txn) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.commit(
				null, new ReactorCommitListener(sink), txn));
	}

	@Override
	public Mono<AbortStatus> abort(Txn txn) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.abort(
				null, new ReactorAbortListener(sink), txn));
	}

	@Override
	public final Flux<KeyRecord> query(Statement statement) throws AerospikeException {
		return query(null, statement);
	}

	@Override
	public final Flux<KeyRecord> query(QueryPolicy policy, Statement statement) throws AerospikeException {
		return Flux.create(sink -> aerospikeClient.query(
				null, new ReactorRecordSequenceListener(sink), policy, statement));
	}

	@Override
	public final Flux<KeyRecord> scanAll(String namespace, String setName, String... binNames) throws AerospikeException {
		return scanAll(null, namespace, setName, binNames);
	}

	@Override
	public final Flux<KeyRecord> scanAll(ScanPolicy policy, String namespace, String setName, String... binNames) throws AerospikeException {
		return Flux.create(sink -> aerospikeClient.scanAll(
				null, new ReactorRecordSequenceListener(sink),
				policy, namespace, setName, binNames));
	}

	@Override
	public final Mono<KeyObject> execute(Key key, String packageName, String functionName, Value... functionArgs) throws AerospikeException {
		return execute(null, key, packageName, functionName, functionArgs);
	}

	@Override
	public final Mono<KeyObject> execute(WritePolicy policy, Key key,
								   String packageName, String functionName, Value... functionArgs) throws AerospikeException {
		return Mono.create(sink -> aerospikeClient.execute(
				null, new ReactorExecuteListener(sink),
				policy, key, packageName, functionName, functionArgs));
	}

	@Override
	public Mono<String> info(InfoPolicy infoPolicy, Node node, String command){
		return info(infoPolicy, node, singletonList(command))
				.flatMap(resultMap -> {
					if(resultMap.containsKey(command)){
						String result = resultMap.get(command);
						return Mono.just(result != null ? result : "");
					} else {
						return Mono.error(new AerospikeException(String.format("Unknown info command: [%s]", command)));
					}
				});
	}

	@Override
	public Mono<Map<String,String>> info(InfoPolicy infoPolicy, Node node, List<String> commands){
		return Mono.create(sink -> aerospikeClient.info(null,
				new ReactorInfoListener(sink), infoPolicy, node, commands.toArray(new String[0])));
    }

	@Override
	public Mono<Void> createIndex(Policy policy,
								  String namespace, String setName, String indexName, String binName,
								  IndexType indexType, IndexCollectionType indexCollectionType, CTX... ctx){
		return waitTillComplete(
				createIndexImpl(policy, namespace, setName, indexName, binName, indexType, indexCollectionType, ctx),
				policy != null ? new InfoPolicy(policy) : aerospikeClient.getInfoPolicyDefault());
	}

	@Override
	public Mono<Void> dropIndex(Policy policy, String namespace, String setName, String indexName){
		return waitTillComplete(
				dropIndexImpl(policy, namespace, setName, indexName),
				policy != null ? new InfoPolicy(policy) : aerospikeClient.getInfoPolicyDefault());
	}

	@Override
	public IAerospikeClient getAerospikeClient() {
		return aerospikeClient;
	}

	@SuppressWarnings("java:S107")
	private Mono<AsyncIndexTask> createIndexImpl(Policy policy,
											 String namespace, String setName, String indexName, String binName,
											 IndexType indexType, IndexCollectionType indexCollectionType, CTX... ctx){
		return Mono.create(sink -> aerospikeClient.createIndex(null,
				new ReactorIndexListener(sink), policy, namespace, setName, indexName, binName,
				indexType, indexCollectionType, ctx));
	}

	private Mono<AsyncIndexTask> dropIndexImpl(Policy policy,
												 String namespace, String setName, String indexName){
		return  Mono.create(sink -> aerospikeClient.dropIndex(null,
				new ReactorIndexListener(sink), policy, namespace, setName, indexName));
	}

	private Mono<Void> waitTillComplete(Mono<AsyncIndexTask> asyncIndexTaskMono, InfoPolicy infoPolicy){
		 return asyncIndexTaskMono.flatMapMany(indexTask ->
				Flux.fromArray(aerospikeClient.getNodes())
						.filter(Node::isActive)
						.flatMap(node -> queryIndexStatus(infoPolicy, indexTask, node)
								.delayElement(Duration.ofMillis(1000))
								.repeat()
								.takeWhile(status -> status == Task.IN_PROGRESS)
						)).then();
	}

	private Mono<Integer> queryIndexStatus(InfoPolicy infoPolicy, AsyncIndexTask indexTask, Node node){
		return Mono.create(sink -> indexTask.queryStatus(null, infoPolicy, node,
				new ReactorTaskStatusListener(sink)));
	}

	@Override
	public Policy getReadPolicyDefault() {
		return aerospikeClient.getReadPolicyDefault();
	}

	@Override
	public WritePolicy getWritePolicyDefault() {
		return aerospikeClient.getWritePolicyDefault();
	}

	@Override
	public ScanPolicy getScanPolicyDefault() {
		return aerospikeClient.getScanPolicyDefault();
	}

	@Override
	public QueryPolicy getQueryPolicyDefault() {
		return aerospikeClient.getQueryPolicyDefault();
	}

	@Override
	public BatchPolicy getBatchPolicyDefault() {
		return aerospikeClient.getBatchPolicyDefault();
	}

	@Override
	public InfoPolicy getInfoPolicyDefault() {
		return aerospikeClient.getInfoPolicyDefault();
	}

	@Override
	public TxnVerifyPolicy getTxnVerifyPolicyDefault() {
		return aerospikeClient.getTxnVerifyPolicyDefault();
	}

	@Override
	public TxnRollPolicy getTxnRollPolicyDefault() {
		return aerospikeClient.getTxnRollPolicyDefault();
	}
}
