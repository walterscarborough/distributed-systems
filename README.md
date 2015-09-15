# distributed-systems
2. Prove the following for vector clocks: 
S t iff (s.v[s.p] ≤ t.v[s.p]) ^(s.v[t.p] < t.v[t.p]) 

S  t	 => 	s ≠ t & t   s
IF: s.v[s.p] ≤ t.v[s.p]	 =>	 for all k(s.v[k] ≤t.v[k])[s.p]
AND: s.p=t.p
THEN: s.v[t.p]  t.v[t.p]	 & for all j (s.v[j] < t.v[j])[t.p]

THEREFORE: s t (s.v[s.p] ≤ t.v[s.p])^(s.v[t.p]<t.v[t.p])

 
3. Some applications require two types of accesses to the critical section –read access and write access for these applications it is reasonable for multiple read accesses to happen concurrently. However, a write access cannot happen concurrently with either a read access or a write access. Modify Lamports’ Mutex algorithm for such applications.

Each process has:	A logical clock vector Vc
			A queue vector Vq
			An access type vector Vt

Each process sends their access type Vt in addition to their logical clock Vc to the queue Vq when they need access to the critical section

1.	Processes can read at any time when there are no write requests in front of them on the queue Vq
2.	Processes can write when it is their turn in the queue Vq
3.	No other process can access the Critical Section while a process is writing to the Critical Section	
