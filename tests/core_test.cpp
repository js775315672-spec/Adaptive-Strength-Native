#include "tracker.hpp"
#include <iostream>
#include <random>
#include <chrono>
#include <cstdlib>
using namespace ase;
static int n=0;void check(bool v,const char* name){std::cout<<(v?"PASS ":"FAIL ")<<name<<"\n";if(!v)std::exit(1);++n;}
constexpr int W=160,H=160;using Frame=std::vector<uint8_t>;
Frame scene(){std::mt19937 g(42);Frame f(W*H);for(auto& x:f)x=g()%256;return f;}
Frame shift(const Frame& f,int x,int y){Frame o(W*H,127);for(int j=0;j<H;j++)for(int i=0;i<W;i++)if(i-x>=0&&i-x<W&&j-y>=0&&j-y<H)o[j*W+i]=f[(j-y)*W+i-x];return o;}
int main(){auto a=scene();Frame blank(W*H,127);Tracker t;check(!t.seed(blank.data(),W,H,80,80,24),"low texture seed refused");check(!t.seed(a.data(),W,H,0,0,24),"edge seed refused");
for(auto [dx,dy]:std::vector<std::pair<int,int>>{{0,0},{3,-4},{9,-9},{12,12},{-12,11},{-7,-10}}){check(t.seed(a.data(),W,H,80,80,24),"seed");auto b=shift(a,dx,dy);auto p=t.step(b.data(),W,H);check(p.valid&&std::hypot(p.x-80-dx,p.y-80-dy)<.01,"known integer displacement");}
check(t.seed(a.data(),W,H,80,80,24),"recovery seed");check(!t.step(blank.data(),W,H).valid,"blank frame rejected");auto b=shift(a,15,13);auto p=t.step(b.data(),W,H);check(p.valid&&std::hypot(p.x-95,p.y-93)<.01,"expanded recovery after gap");
check(t.seed(a.data(),W,H,80,80,24),"occlusion seed");b=shift(a,5,3);for(int y=78;y<89;y++)for(int x=80;x<91;x++)b[y*W+x]=127;p=t.step(b.data(),W,H);check(p.valid&&std::hypot(p.x-85,p.y-83)<.01,"center occlusion consensus");
check(t.seed(a.data(),W,H,80,80,24),"brightness seed");b=shift(a,2,-2);for(auto& x:b)x=uint8_t(20+x*.7);p=t.step(b.data(),W,H);check(p.valid&&std::hypot(p.x-82,p.y-78)<.01,"brightness change");
check(t.seed(a.data(),W,H,80,80,24),"stop seed");for(int i=0;i<8;i++)check(!t.step(blank.data(),W,H).valid,"missing frame refused");check(!t.step(a.data(),W,H).valid,"eight misses fail closed");
check(t.seed(a.data(),W,H,80,80,24),"dimension seed");check(!t.step(a.data(),W-1,H).valid,"dimension mismatch refused");
// 100 independently shifted frames; desktop only.
t.seed(a.data(),W,H,80,80,24);auto start=std::chrono::steady_clock::now();int good=0;for(int i=0;i<100;i++){int x=(i%13)-6,y=(i%9)-4;auto f=shift(a,x,y);auto q=t.step(f.data(),W,H);if(q.valid&&std::hypot(q.x-80-x,q.y-80-y)<.01)good++;}auto ms=std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-start).count();check(good==100,"100-frame regression");std::cout<<"HOST_TRACKER_BENCHMARK frames=100 ms="<<ms<<" (includes synthetic generation)\nCXX_PASS="<<n<<"\n";
}
