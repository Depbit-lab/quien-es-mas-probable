(() => {
  'use strict';

  const APP_VERSION = '0.2.0';
  const APP_TITLE = '¿Quién es más probable que…?';
  const QUESTION_TAG = 'sinfiltro-question-v1';
  const VOTE_TAG = 'sinfiltro-vote-v1';
  const EVENT_KIND = 30078;
  const DEFAULT_RELAYS = [
    'wss://relay.damus.io',
    'wss://nos.lol',
    'wss://relay.primal.net'
  ];
  const CATEGORIES = [...new Set(window.BUILTIN_QUESTIONS.map(q => q.category))];

  const STORAGE = {
    custom: 'sf_custom_questions_v2',
    votes: 'sf_local_votes_v2',
    communityQuestions: 'sf_community_questions_v2',
    voteEvents: 'sf_vote_events_v2',
    pending: 'sf_pending_events_v2',
    relays: 'sf_relays_v2',
    used: 'sf_used_v2'
  };

  const els = Object.fromEntries([
    'mode','noRepeat','badge','question','remaining','rankInfo','upvote','downvote','upCount','downCount',
    'next','addQuestion','shareQuestion','sync','shareApp','communityStatus','statusText','addDialog','addForm',
    'newQuestion','newCategory','publishQuestion','communityDialog','closeCommunity','relayInput','resetRelays',
    'saveRelays','communityQuestions','communityVotes','pendingCount','toast'
  ].map(id => [id, document.getElementById(id)]));

  let current = null;
  let used = new Set(load(STORAGE.used, []));
  let localVotes = load(STORAGE.votes, {});
  let customQuestions = load(STORAGE.custom, []);
  let communityQuestions = load(STORAGE.communityQuestions, []);
  let voteEvents = load(STORAGE.voteEvents, {}); // key pubkey:question -> event summary
  let pendingEvents = load(STORAGE.pending, []);
  let relays = load(STORAGE.relays, DEFAULT_RELAYS);
  let syncing = false;
  let toastTimer;
  const pubkey = nativeCall('getPubkey') || 'browser-local';

  CATEGORIES.forEach(category => {
    const option = document.createElement('option');
    option.value = category;
    option.textContent = category;
    els.mode.append(option);
    const option2 = option.cloneNode(true);
    els.newCategory.append(option2);
  });

  function load(key, fallback) {
    try { const value = JSON.parse(localStorage.getItem(key)); return value ?? fallback; }
    catch (_) { return fallback; }
  }
  function save(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch (_) {} }
  function nativeCall(method, ...args) {
    try { return window.Android && typeof window.Android[method] === 'function' ? window.Android[method](...args) : null; }
    catch (_) { return null; }
  }
  function toast(message) {
    clearTimeout(toastTimer); els.toast.textContent = message; els.toast.classList.add('show');
    toastTimer = setTimeout(() => els.toast.classList.remove('show'), 2300);
  }
  function cleanText(text) { return text.trim().replace(/\s+/g, ' '); }
  function now() { return Math.floor(Date.now() / 1000); }
  function uuid() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 9); }
  function allQuestions() {
    const map = new Map();
    [...window.BUILTIN_QUESTIONS, ...communityQuestions, ...customQuestions].forEach(q => {
      if (q && q.id && q.text && !map.has(q.id)) map.set(q.id, q);
    });
    return [...map.values()];
  }
  function latestVotesByQuestion() {
    const counts = {};
    Object.values(voteEvents).forEach(v => {
      if (!v || !v.questionId || ![1,-1].includes(v.value)) return;
      counts[v.questionId] ||= {up:0,down:0};
      v.value === 1 ? counts[v.questionId].up++ : counts[v.questionId].down++;
    });
    // Ensure offline vote is visible even before relay publication.
    Object.entries(localVotes).forEach(([questionId,value]) => {
      const key = `${pubkey}:${questionId}`;
      if (!voteEvents[key]) {
        counts[questionId] ||= {up:0,down:0};
        value === 1 ? counts[questionId].up++ : counts[questionId].down++;
      }
    });
    return counts;
  }
  function countsFor(id) { return latestVotesByQuestion()[id] || {up:0,down:0}; }
  function wilson(up, down) {
    const n = up + down; if (!n) return 0;
    const z = 1.96, p = up / n;
    return (p + z*z/(2*n) - z*Math.sqrt((p*(1-p)+z*z/(4*n))/n)) / (1+z*z/n);
  }
  function categoryAffinity() {
    const scores = Object.fromEntries(CATEGORIES.map(c => [c,0]));
    const byId = new Map(allQuestions().map(q => [q.id,q]));
    Object.entries(localVotes).forEach(([id,v]) => {
      const q=byId.get(id); if(q) scores[q.category]=(scores[q.category]||0)+(v===1?1:-0.65);
    });
    return scores;
  }
  function filteredQuestions() {
    let pool = allQuestions();
    const mode = els.mode.value;
    const counts = latestVotesByQuestion();
    if (CATEGORIES.includes(mode)) pool = pool.filter(q => q.category === mode);
    if (mode === 'Popular') {
      const popular = pool.filter(q => (counts[q.id]?.up||0)+(counts[q.id]?.down||0)>=3)
        .sort((a,b) => wilson(counts[b.id]?.up||0,counts[b.id]?.down||0)-wilson(counts[a.id]?.up||0,counts[a.id]?.down||0));
      pool = popular.length ? popular : pool;
    }
    if (mode === 'Nuevas') pool = pool.filter(q => q.source !== 'builtin').sort((a,b)=>(b.createdAt||0)-(a.createdAt||0));
    if (mode === 'Favoritas') pool = pool.filter(q => localVotes[q.id] === 1);
    return pool;
  }
  function weightedPick(pool) {
    const counts = latestVotesByQuestion();
    const affinity = categoryAffinity();
    const mode = els.mode.value;
    const weighted = pool.map(q => {
      const c=counts[q.id]||{up:0,down:0};
      const quality=wilson(c.up,c.down);
      let weight=1;
      if(mode==='Para ti') weight += quality*4 + Math.max(-.6,Math.min(3,(affinity[q.category]||0)*.35)) + (q.source!=='builtin'?.6:0);
      if(mode==='Popular') weight += quality*7 + Math.log1p(c.up+c.down);
      if(mode==='Nuevas') weight += q.source!=='builtin'?2:0;
      return [q,Math.max(.15,weight)];
    });
    let r=Math.random()*weighted.reduce((s,x)=>s+x[1],0);
    for(const [q,w] of weighted){ r-=w; if(r<=0)return q; }
    return weighted.at(-1)?.[0];
  }
  function showEmptyState(badge,message){
    current=null;
    els.question.textContent=message;
    els.question.classList.add('empty');
    els.badge.textContent=badge;
    els.next.textContent='Sacar pregunta';
    updateCard();
  }
  function drawQuestion() {
    // Sin votos positivos todavia, «Mis favoritas» explica como se llena en vez de quedarse en blanco.
    if(els.mode.value==='Favoritas'&&!filteredQuestions().length){
      showEmptyState('Mis favoritas','Juega y vota antes. Cuando una pregunta te guste, márcala con ▲ y se guardará aquí para la próxima partida.');
      return;
    }
    let pool=filteredQuestions();
    if(els.noRepeat.checked) pool=pool.filter(q=>!used.has(q.id));
    if(!pool.length){ used.clear(); save(STORAGE.used,[]); pool=filteredQuestions(); }
    if(!pool.length){ toast('Todavía no hay preguntas en este filtro'); return; }
    current=weightedPick(pool);
    used.add(current.id); save(STORAGE.used,[...used]);
    els.question.textContent=current.text;
    els.question.classList.remove('empty');
    els.badge.textContent=current.category;
    els.next.textContent='Otra pregunta';
    updateCard();
    if(navigator.vibrate) navigator.vibrate(30);
  }
  function updateCard() {
    const pool=filteredQuestions();
    const available=els.noRepeat.checked?pool.filter(q=>!used.has(q.id)).length:pool.length;
    els.remaining.textContent=`${available} disponibles`;
    const c=current?countsFor(current.id):{up:0,down:0};
    els.upCount.textContent=c.up; els.downCount.textContent=c.down;
    els.upvote.className='vote-button'+(current&&localVotes[current.id]===1?' active up':'');
    els.downvote.className='vote-button'+(current&&localVotes[current.id]===-1?' active down':'');
    const n=c.up+c.down;
    els.rankInfo.textContent=n?`${n} voto${n===1?'':'s'} · ${Math.round(c.up/n*100)}% positivos`:'Sin votos todavía';
    updateCommunityStats();
  }
  async function vote(value) {
    if(!current){ toast('Saca una pregunta primero'); return; }
    const qid=current.id;
    const previous=localVotes[qid];
    localVotes[qid]=previous===value?0:value;
    if(localVotes[qid]===0) delete localVotes[qid];
    save(STORAGE.votes,localVotes);
    const selected=localVotes[qid]||0;
    const key=`${pubkey}:${qid}`;
    if(selected) voteEvents[key]={pubkey,questionId:qid,value:selected,created_at:now(),local:true};
    else delete voteEvents[key];
    save(STORAGE.voteEvents,voteEvents); updateCard();
    if(selected && hasNativeSigning()) {
      const event=signEvent({kind:EVENT_KIND,created_at:now(),tags:[['d',`v:${qid}`],['t',VOTE_TAG],['q',qid],['client','sinfiltro-android']],content:JSON.stringify({questionId:qid,value:selected,version:1})});
      if(event){ queueEvent(event); const ok=await publishEvent(event,false); if(ok) removePending(event.id); }
    }
  }
  function hasNativeSigning(){ return !!(window.Android&&typeof window.Android.signEvent==='function'); }
  function signEvent(template){
    try {
      const canonical=JSON.stringify([0,pubkey,template.created_at,template.kind,template.tags,template.content]);
      const raw=nativeCall('signEvent',JSON.stringify({...template,canonical})); return raw?JSON.parse(raw):null;
    }
    catch(_){ toast('No se pudo firmar el evento'); return null; }
  }
  function verifyEvent(event){
    try {
      const canonical=JSON.stringify([0,event.pubkey,event.created_at,event.kind,event.tags,event.content]);
      return nativeCall('verifyEvent',JSON.stringify(event),canonical)===true; }
    catch(_){ return false; }
  }
  function queueEvent(event){ if(!pendingEvents.some(e=>e.id===event.id)){pendingEvents.push(event);save(STORAGE.pending,pendingEvents);} updateCommunityStats(); }
  function removePending(id){ pendingEvents=pendingEvents.filter(e=>e.id!==id); save(STORAGE.pending,pendingEvents); updateCommunityStats(); }

  function relayQuery(url,timeout=6500){
    return new Promise(resolve=>{
      const events=[]; let done=false; const sub='sf'+Math.random().toString(36).slice(2);
      let ws; const finish=()=>{if(done)return;done=true;try{ws.close()}catch(_){}resolve({url,events,ok:events.length>=0});};
      const timer=setTimeout(finish,timeout);
      try{
        ws=new WebSocket(url);
        ws.onopen=()=>ws.send(JSON.stringify(['REQ',sub,{kinds:[EVENT_KIND],'#t':[QUESTION_TAG],limit:600},{kinds:[EVENT_KIND],'#t':[VOTE_TAG],limit:4000}]));
        ws.onmessage=msg=>{try{const data=JSON.parse(msg.data);if(data[0]==='EVENT'&&data[1]===sub)events.push(data[2]);if(data[0]==='EOSE'&&data[1]===sub){clearTimeout(timer);finish();}}catch(_){}};
        ws.onerror=()=>{}; ws.onclose=()=>{clearTimeout(timer);finish();};
      }catch(_){clearTimeout(timer);finish();}
    });
  }
  function relayPublish(url,event,timeout=5000){
    return new Promise(resolve=>{
      let done=false,ws; const finish=ok=>{if(done)return;done=true;try{ws.close()}catch(_){}resolve(ok)}; const timer=setTimeout(()=>finish(false),timeout);
      try{
        ws=new WebSocket(url);
        ws.onopen=()=>ws.send(JSON.stringify(['EVENT',event]));
        ws.onmessage=msg=>{try{const d=JSON.parse(msg.data);if(d[0]==='OK'&&d[1]===event.id){clearTimeout(timer);finish(!!d[2]);}}catch(_){}};
        ws.onerror=()=>{}; ws.onclose=()=>{clearTimeout(timer);finish(false)};
      }catch(_){clearTimeout(timer);finish(false)}
    });
  }
  async function publishEvent(event,show=true){
    if(!navigator.onLine){ if(show)toast('Guardado. Se publicará cuando haya conexión'); return false; }
    const results=await Promise.allSettled(relays.map(r=>relayPublish(r,event)));
    const accepted=results.filter(r=>r.status==='fulfilled'&&r.value).length;
    if(show) toast(accepted?`Publicado en ${accepted} relay${accepted===1?'':'s'}`:'Guardado localmente. Ningún relay respondió');
    return accepted>0;
  }
  function parseQuestionEvent(event){
    if(event.kind!==EVENT_KIND||!event.tags.some(t=>t[0]==='t'&&t[1]===QUESTION_TAG)||!event.tags.some(t=>t[0]==='d'&&String(t[1]||'').startsWith('q:'))||!verifyEvent(event))return null;
    try{
      const data=JSON.parse(event.content); const text=cleanText(data.text||''); const category=CATEGORIES.includes(data.category)?data.category:'Nuclear';
      if(!data.id||text.length<18||text.length>220||!text.startsWith('¿'))return null;
      return {id:data.id,text,category,source:'community',createdAt:event.created_at||0,author:event.pubkey,eventId:event.id};
    }catch(_){return null}
  }
  function parseVoteEvent(event){
    if(event.kind!==EVENT_KIND||!event.tags.some(t=>t[0]==='t'&&t[1]===VOTE_TAG)||!event.tags.some(t=>t[0]==='d'&&String(t[1]||'').startsWith('v:'))||!verifyEvent(event))return null;
    try{const d=JSON.parse(event.content);if(!d.questionId||![1,-1].includes(d.value))return null;return{pubkey:event.pubkey,questionId:d.questionId,value:d.value,created_at:event.created_at,eventId:event.id}}catch(_){return null}
  }
  function mergeEvents(events){
    const qMap=new Map(communityQuestions.map(q=>[q.id,q]));
    let acceptedQ=0,acceptedV=0;
    events.forEach(event=>{
      const q=parseQuestionEvent(event); if(q){const old=qMap.get(q.id);if(!old||(q.createdAt||0)>=(old.createdAt||0)){qMap.set(q.id,q);acceptedQ++;}return;}
      const v=parseVoteEvent(event); if(v){const key=`${v.pubkey}:${v.questionId}`;const old=voteEvents[key];if(!old||v.created_at>=old.created_at){voteEvents[key]=v;acceptedV++;}}
    });
    communityQuestions=[...qMap.values()].slice(-1000);
    save(STORAGE.communityQuestions,communityQuestions);save(STORAGE.voteEvents,voteEvents);updateCard();
    return {acceptedQ,acceptedV};
  }
  async function syncCommunity(showToast=true){
    if(syncing)return; if(!navigator.onLine){toast('No hay conexión');return;}
    if(!hasNativeSigning()){toast('La sincronización firmada funciona dentro de la app Android');return;}
    syncing=true; setStatus('syncing','Sincronizando');
    const results=await Promise.all(relays.map(r=>relayQuery(r)));
    const all=results.flatMap(r=>r.events); const unique=[...new Map(all.map(e=>[e.id,e])).values()];
    const merged=mergeEvents(unique);
    let sent=0;
    for(const event of [...pendingEvents]){ if(await publishEvent(event,false)){removePending(event.id);sent++;} }
    syncing=false; setStatus(results.some(r=>r.events.length||r.ok)?'online':'offline',results.filter(r=>r.ok).length?`${results.filter(r=>r.ok).length} relays`:'Local');
    if(showToast)toast(`Sincronizado: ${merged.acceptedQ} preguntas, ${merged.acceptedV} votos${sent?`, ${sent} pendientes enviados`:''}`);
  }
  function setStatus(state,text){els.communityStatus.className=`status ${state}`;els.statusText.textContent=text;}

  async function addQuestion(){
    const text=cleanText(els.newQuestion.value); const category=els.newCategory.value;
    if(text.length<18){toast('La pregunta es demasiado corta');return;}
    if(!text.startsWith('¿')){toast('Empieza la pregunta con ¿');return;}
    const normalized=text.toLocaleLowerCase('es-ES').replace(/[^a-záéíóúüñ0-9]+/g,' ').trim();
    if(allQuestions().some(q=>q.text.toLocaleLowerCase('es-ES').replace(/[^a-záéíóúüñ0-9]+/g,' ').trim()===normalized)){toast('Esa pregunta ya existe');return;}
    const id=`n_${pubkey.slice(0,12)}_${uuid()}`;
    const q={id,text,category,source:'custom',createdAt:now(),author:pubkey,pending:true};
    customQuestions.push(q);save(STORAGE.custom,customQuestions);els.newQuestion.value='';els.addDialog.close();current=q;els.question.textContent=q.text;els.badge.textContent=q.category;updateCard();toast('Pregunta añadida al móvil');
    if(hasNativeSigning()){
      const event=signEvent({kind:EVENT_KIND,created_at:now(),tags:[['d',`q:${id}`],['t',QUESTION_TAG],['category',category],['client','sinfiltro-android']],content:JSON.stringify({id,text,category,version:1})});
      if(event){queueEvent(event);const ok=await publishEvent(event,true);if(ok){removePending(event.id);q.pending=false;save(STORAGE.custom,customQuestions);}}
    }
  }
  function shareText(title,text){
    if(nativeCall('shareText',title,text)!==null)return;
    if(navigator.share)navigator.share({title,text}).catch(()=>{});else window.prompt('Copia este texto:',text);
  }
  function updateCommunityStats(){
    els.communityQuestions.textContent=communityQuestions.length+customQuestions.length;
    els.communityVotes.textContent=Object.keys(voteEvents).length;
    els.pendingCount.textContent=pendingEvents.length;
  }
  function openCommunity(){els.relayInput.value=relays.join('\n');updateCommunityStats();els.communityDialog.showModal();}
  function saveRelaySettings(){
    const values=els.relayInput.value.split(/\s+/).map(x=>x.trim()).filter(x=>/^wss:\/\//.test(x));
    if(!values.length){toast('Añade al menos un relay wss://');return;}
    relays=[...new Set(values)].slice(0,8);save(STORAGE.relays,relays);els.communityDialog.close();syncCommunity();
  }

  els.next.addEventListener('click',drawQuestion);
  els.upvote.addEventListener('click',()=>vote(1));
  els.downvote.addEventListener('click',()=>vote(-1));
  els.mode.addEventListener('change',()=>{used.clear();save(STORAGE.used,[]);drawQuestion()});
  els.noRepeat.addEventListener('change',updateCard);
  els.addQuestion.addEventListener('click',()=>els.addDialog.showModal());
  els.addForm.addEventListener('submit',e=>{e.preventDefault();addQuestion()});
  els.shareQuestion.addEventListener('click',()=>current?shareText(APP_TITLE,`${current.text}\n\n${APP_TITLE} · Vota tú también.`):toast('Saca una pregunta primero'));
  els.sync.addEventListener('click',()=>syncCommunity());
  els.shareApp.addEventListener('click',()=>{if(nativeCall('shareApp')===null)toast('El APK se puede compartir desde la aplicación Android')});
  els.communityStatus.addEventListener('click',openCommunity);
  els.closeCommunity.addEventListener('click',()=>els.communityDialog.close());
  els.saveRelays.addEventListener('click',saveRelaySettings);
  els.resetRelays.addEventListener('click',()=>{els.relayInput.value=DEFAULT_RELAYS.join('\n')});
  window.addEventListener('online',()=>syncCommunity(false));
  window.addEventListener('offline',()=>setStatus('offline','Local'));

  updateCard();
  setStatus(navigator.onLine&&hasNativeSigning()?'online':'offline',navigator.onLine&&hasNativeSigning()?'Comunidad':'Local');
  drawQuestion();
  if(navigator.onLine&&hasNativeSigning())setTimeout(()=>syncCommunity(false),900);
})();
