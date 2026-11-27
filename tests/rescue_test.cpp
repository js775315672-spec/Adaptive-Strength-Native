#include "tracker.hpp"
#include <random>
#include <iostream>
#include <cstdlib>
using namespace ase;
constexpr int W=180,H=180; using Frame=std::vector<uint8_t>;
int n=0;void check(bool ok,const char* what){std::cout<<(ok?"PASS ":"FAIL ")<<what<<"\n";if(!ok)std::exit(1);++n;}
Frame shift(const Frame& a,int dx,int dy){Frame b(W*H,127);for(int y=0;y<H;y++)for(int x=0;x<W;x++){int xx=x-dx,yy=y-dy;if(xx>=0&&xx<W&&yy>=0&&yy<H)b[y*W+x]=a[yy*W+xx];}return b;}
int main(){Frame a(W*H);std::mt19937 g(42);for(auto& v:a)v=g()%256;Tracker t;
 for(auto [dx,dy]:std::vector<std::pair<int,int>>{{24,18},{-29,15},{18,-30},{30,30}}){check(t.seed(a.data(),W,H,90,90,24),"wide rescue seed");auto b=shift(a,dx,dy);auto p=t.step(b.data(),W,H);check(p.valid&&std::hypot(p.x-90-dx,p.y-90-dy)<.01,"wide one-frame jump recovered");}
 Frame blank(W*H,127);check(t.seed(a.data(),W,H,90,90,24),"blank rescue seed");check(!t.step(blank.data(),W,H).valid,"blank rejected by rescue");
 Frame dup(W*H,127);for(int y=-18;y<=18;y++)for(int x=-18;x<=18;x++){dup[(90+y)*W+90-29+x]=a[(90+y)*W+90+x];dup[(90+y)*W+90+29+x]=a[(90+y)*W+90+x];}check(t.seed(a.data(),W,H,90,90,24),"ambiguous rescue seed");check(!t.step(dup.data(),W,H).valid,"two identical distant candidates rejected");
 check(t.seed(a.data(),W,H,90,90,24),"outside rescue seed");auto b=shift(a,40,40);check(!t.step(b.data(),W,H).valid,"out of window rejected");
 std::cout<<"P8_RESCUE_PASS="<<n<<"\n";
}
